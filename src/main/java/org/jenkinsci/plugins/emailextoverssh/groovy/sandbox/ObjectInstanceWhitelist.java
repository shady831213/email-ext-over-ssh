package org.jenkinsci.plugins.emailextoverssh.groovy.sandbox;

import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.AbstractWhitelist;

import javax.annotation.CheckForNull;
import java.lang.ref.WeakReference;

/**
 * A {@link Whitelist} that permits methods on specific object instances.
 */
public abstract class ObjectInstanceWhitelist<T> extends AbstractWhitelist {
    private final WeakReference<T> instance;

    protected ObjectInstanceWhitelist(T instance) {
        this.instance = new WeakReference<>(instance);
    }

    protected synchronized boolean permitsInstance(@CheckForNull Object instance) {
        return this.instance.get() == instance;
    }

    protected boolean isClass(Class<?> declaringClass) {
        T t = instance.get();
        if (t != null) {
            Class c = t.getClass();
            while (c != null && c != Object.class) {
                if (declaringClass == c) {
                    return true;
                } else {
                    if (isInterface(declaringClass, c.getInterfaces())) {
                        return true;
                    }
                    c = c.getSuperclass();
                }
            }
        }
        return false;
    }

    private boolean isInterface(Class<?> declaringClass, Class[] interfaces) {
        for (Class interf : interfaces) {
            if (declaringClass == interf) {
                return true;
            }
            if (isInterface(declaringClass, interf.getInterfaces())) {
                return true;
            }
        }
        return false;
    }
}
