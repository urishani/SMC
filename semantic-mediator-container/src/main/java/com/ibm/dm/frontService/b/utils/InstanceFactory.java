
/*
  Copyright 2011-2016 IBM
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

*/
/*
 *+------------------------------------------------------------------------+
 
 
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

package com.ibm.dm.frontService.b.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

public class InstanceFactory
{
    public static final String GENERATE_RANDOM_INSTANCE_METHOD_NAME = "generateRandom";

    public InstanceFactory()
    {}

    @SuppressWarnings("unchecked")
    static public <T> T generateRandom(Class<T> clazz) throws InstantiationException, IllegalAccessException
    {
        try
        {
            Method m = clazz.getMethod(GENERATE_RANDOM_INSTANCE_METHOD_NAME);
            return (T) m.invoke(null);

        }
        catch (Exception e)
        {
            return clazz.newInstance();
        }

    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> generate(Class<T> clazz)
    {
        final int COUNT = 5;
        Collection<T> c = new ArrayList<T>(COUNT);
        try
        {
            Method m = clazz.getMethod(GENERATE_RANDOM_INSTANCE_METHOD_NAME);
            for (int i = 0; i < COUNT; i++)
            {
                c.add((T) m.invoke(null));
            }
        }
        catch (Exception e)
        {
            try
            {
                for (int i = 0; i < COUNT; i++)
                {
                    c.add(clazz.newInstance());
                }
            }
            catch (InstantiationException ee)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException ee)
            {
                e.printStackTrace();
            }
        }
        return c;

    }

    //    private Collection<Item> col()
    //    {
    //        ArrayList<Item> c = new ArrayList<Item>();
    //        for (int i = 0; i < 5; i++)
    //            c.add(Item.generateRandom());
    //        return c;
    //    }
    //
    //    private <T> Model f(Collection<T> xx)
    //    {
    //
    //        Model model = ModelFactory.createDefaultModel();
    //        Bean2RDF writer = new Bean2RDF(model);
    //        for (T x : xx)
    //            writer.saveDeep(x);
    //        return model;
    //    }

}

