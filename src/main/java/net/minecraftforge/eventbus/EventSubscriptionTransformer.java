/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Optional;

import static net.minecraftforge.eventbus.Logging.EVENTBUS;
import static net.minecraftforge.eventbus.Names.CANCELLABLE;
import static net.minecraftforge.eventbus.Names.HAS_RESULT;
import static net.minecraftforge.eventbus.Names.LISTENER_LIST;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class EventSubscriptionTransformer
{

    private static final Logger LOGGER = LogManager.getLogger("EVENTBUS");

    public Optional<ClassNode> transform(final ClassNode classNode, final Type classType)
    {
        try
        {
            if (!buildEvents(classNode)) return Optional.empty();
        }
        catch (ClassNotFoundException ex)
        {
            // Discard silently- it's just noise
        }
        catch (Exception e)
        {
            LOGGER.error(EVENTBUS, "An error occurred building event handler", e);
        }
        return Optional.of(classNode);
    }

    private boolean buildEvents(ClassNode classNode) throws Exception
    {
        // Yes, this recursively loads classes until we get this base class. THIS IS NOT A ISSUE. Coremods should handle re-entry just fine.
        // If they do not this a COREMOD issue NOT a Forge/LaunchWrapper issue.
        // well, we should at least use the context classloader - this is forcing all the game classes in through
        // the system classloader otherwise...
        Class<?> parent = Thread.currentThread().getContextClassLoader().loadClass(classNode.superName.replace('/', '.'));
        if (!Event.class.isAssignableFrom(parent))
        {
            return false;
        }

        LOGGER.debug(EVENTBUS, "Event transform begin: {}", classNode.name);
        //Class<?> listenerListClazz = Class.forName("net.minecraftforge.fml.common.eventhandler.ListenerList", false, getClass().getClassLoader());
        Type tList = Type.getType(LISTENER_LIST);

        boolean hasSetup           = false;
        boolean hasGetListenerList = false;
        boolean hasDefaultCtr      = false;
        boolean hasCancelable      = false;
        boolean hasResult          = false;
        String voidDesc            = Type.getMethodDescriptor(VOID_TYPE);
        String boolDesc            = Type.getMethodDescriptor(BOOLEAN_TYPE);
        String listDesc            = tList.getDescriptor();
        String listDescM           = Type.getMethodDescriptor(tList);

        for (MethodNode method : classNode.methods)
        {
            if (method.name.equals("setup") && method.desc.equals(voidDesc) && (method.access & ACC_PROTECTED) == ACC_PROTECTED) hasSetup = true;
            if ((method.access & ACC_PUBLIC) == ACC_PUBLIC)
            {
                if (method.name.equals("getListenerList") && method.desc.equals(listDescM)) hasGetListenerList = true;
                if (method.name.equals("isCancelable")    && method.desc.equals(boolDesc))  hasCancelable = true;
                if (method.name.equals("hasResult")       && method.desc.equals(boolDesc))  hasResult = true;
            }
            if (method.name.equals("<init>") && method.desc.equals(voidDesc)) hasDefaultCtr = true;
        }

        if (classNode.visibleAnnotations != null)
        {
            for (AnnotationNode node : classNode.visibleAnnotations)
            {
                if (!hasResult && node.desc.equals(HAS_RESULT))
                {
                    /* Add:
                     *      public boolean hasResult()
                     *      {
                     *            return true;
                     *      }
                     */
                    MethodNode method = new MethodNode(ACC_PUBLIC, "hasResult", boolDesc, null, null);
                    method.instructions.add(new InsnNode(ICONST_1));
                    method.instructions.add(new InsnNode(IRETURN));
                    classNode.methods.add(method);
                }
                else if (!hasCancelable && node.desc.equals(CANCELLABLE))
                {
                    /* Add:
                     *      public boolean isCancelable()
                     *      {
                     *            return true;
                     *      }
                     */
                    MethodNode method = new MethodNode(ACC_PUBLIC, "isCancelable", boolDesc, null, null);
                    method.instructions.add(new InsnNode(ICONST_1));
                    method.instructions.add(new InsnNode(IRETURN));
                    classNode.methods.add(method);
                }
            }
        }

        if (hasSetup)
        {
            if (!hasGetListenerList) {
                LOGGER.error(EVENTBUS, "Event class {} defines a custom setup() method and is missing getListenerList", classNode.name);
                throw new RuntimeException("Event class defines setup() but does not define getListenerList! " + classNode.name);
            } else {
                LOGGER.debug(EVENTBUS, "Transforming event complete - already done: {}", classNode.name);
                return true;
            }
        }

        Type tSuper = Type.getObjectType(classNode.superName);

        //Add private static ListenerList LISTENER_LIST
        classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC, "LISTENER_LIST", listDesc, null, null));

        /*Add:
         *      public <init>()
         *      {
         *              super();
         *      }
         */
        if (!hasDefaultCtr)
        {
            MethodNode method = new MethodNode(ACC_PUBLIC, "<init>", voidDesc, null, null);
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            method.instructions.add(new MethodInsnNode(INVOKESPECIAL, tSuper.getInternalName(), "<init>", voidDesc, false));
            method.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(method);
        }

        /*Add:
         *      protected void setup()
         *      {
         *              super.setup();
         *              if (LISTENER_LIST != NULL)
         *              {
         *                      return;
         *              }
         *              LISTENER_LIST = new ListenerList(super.getListenerList());
         *      }
         */
        MethodNode method = new MethodNode(ACC_PROTECTED, "setup", voidDesc, null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, tSuper.getInternalName(), "setup", voidDesc, false));
        method.instructions.add(new FieldInsnNode(GETSTATIC, classNode.name, "LISTENER_LIST", listDesc));
        LabelNode initListener = new LabelNode();
        method.instructions.add(new JumpInsnNode(IFNULL, initListener));
        method.instructions.add(new InsnNode(RETURN));
        method.instructions.add(initListener);
        method.instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
        method.instructions.add(new TypeInsnNode(NEW, tList.getInternalName()));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, tSuper.getInternalName(), "getListenerList", listDescM, false));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, tList.getInternalName(), "<init>", getMethodDescriptor(VOID_TYPE, tList), false));
        method.instructions.add(new FieldInsnNode(PUTSTATIC, classNode.name, "LISTENER_LIST", listDesc));
        method.instructions.add(new InsnNode(RETURN));
        classNode.methods.add(method);

        /*Add:
         *      public ListenerList getListenerList()
         *      {
         *              return this.LISTENER_LIST;
         *      }
         */
        method = new MethodNode(ACC_PUBLIC, "getListenerList", listDescM, null, null);
        method.instructions.add(new FieldInsnNode(GETSTATIC, classNode.name, "LISTENER_LIST", listDesc));
        method.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(method);
        LOGGER.debug(EVENTBUS, "Event transform complete: {}", classNode.name);
        return true;
    }
}
