/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.launcher.daemon.server.health;

import org.gradle.internal.Cast;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

class MemoryInfo {

    private final long totalMemory; //this does not change

    MemoryInfo() {
        totalMemory = Runtime.getRuntime().maxMemory();
    }

    /**
     * Approx. time spent in gc. See {@link GarbageCollectorMXBean}
     */
    public long getCollectionTime() {
        long garbageCollectionTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long time = gc.getCollectionTime();
            if (time >= 0) {
                garbageCollectionTime += time;
            }
        }
        return garbageCollectionTime;
    }

    /**
     * Max memory that this process can commit in bytes. Always returns the same value because maximum memory is determined at jvm start.
     */
    public long getMaxMemory() {
        return totalMemory;
    }

    /**
     * Currently committed memory of this process in bytes. May return different value depending on how the heap has expanded. The returned value is <= {@link #getMaxMemory()}
     */
    public long getCommittedMemory() {
        //querying runtime for each invocation
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * Retrieves the total physical memory size on the system in bytes. This value is independent of {@link #getMaxMemory()}, which is the total memory available to the JVM.
     *
     * @throws UnsupportedOperationException if the JVM doesn't support getting total physical memory.
     */
    public long getTotalPhysicalMemory() {
        return getMbeanAttribute("java.lang:type=OperatingSystem", "TotalPhysicalMemorySize", Long.class);
    }

    /**
     * Retrieves the free physical memory on the system in bytes. This value is independent of {@link #getCommittedMemory()}, which is the memory reserved by the JVM.
     *
     * @throws UnsupportedOperationException if the JVM doesn't support getting free physical memory.
     */
    public long getFreePhysicalMemory() {
        return getMbeanAttribute("java.lang:type=OperatingSystem", "FreePhysicalMemorySize", Long.class);
    }

    /**
     * Calls an mbean method if available.
     *
     * @throws UnsupportedOperationException if this method isn't available on this JVM.
     */
    private static <T> T getMbeanAttribute(String mbean, final String attribute, Class<T> type) {
        Exception rootCause;
        try {
            ObjectName objectName = new ObjectName(mbean);
            return Cast.cast(type, ManagementFactory.getPlatformMBeanServer().getAttribute(objectName, attribute));
        } catch (InstanceNotFoundException e) {
            rootCause = e;
        } catch (ReflectionException e) {
            rootCause = e;
        } catch (MalformedObjectNameException e) {
            rootCause = e;
        } catch (MBeanException e) {
            rootCause = e;
        } catch (AttributeNotFoundException e) {
           rootCause = e;
        }
        throw new UnsupportedOperationException("(" + mbean + ")." + attribute + " is unsupported on this JVM.", rootCause);
    }
}
