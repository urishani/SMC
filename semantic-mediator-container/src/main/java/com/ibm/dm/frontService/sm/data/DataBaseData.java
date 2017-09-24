
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

package com.ibm.dm.frontService.sm.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import thewebsemantic.Namespace;

import com.ibm.dm.frontService.b.utils.U;

@Namespace(DataBaseData.NAME_SPACE)
public class DataBaseData extends AbstractRDFReadyClass
{
    static public interface IFields extends AbstractRDFReadyClass.IFields
    {
        public static final String COUNTER    = "counter";
        public static final String ONTOLOGIES = "ontologies";
        public static final String PORTS      = "ports";
        public static final String RULES      = "rules";
        public static final String CONFIGS    = "configs";
        public static final String MEDIATORS  = "mediators";
        public static final String CATALOGS  = "catalogs";
    }

    static public enum EClassification {
    	ontologies,
    	rules,
    	ports,
    	mediators,
    	friends,
    	shapes,
    	catalogs,
    }
    
    private static final long serialVersionUID = 1L;
    protected int             counter          = 100;
    protected List<Ontology>  ontologies       = new ArrayList<Ontology>();
    protected List<Port>      ports            = new ArrayList<Port>();
    protected List<RuleSet>   rules            = new ArrayList<RuleSet>();
    protected List<Mediator>  mediators        = new ArrayList<Mediator>();
    protected List<Friend>    friends          = new ArrayList<Friend>();
    protected List<Shape>     shapes           = new ArrayList<Shape>();
    protected List<Catalog>   catalogs         = new ArrayList<Catalog>();
    
    public DataBaseData()
    {
        super();
    }

    public static DataBaseData generateRandom()
    {
        DataBaseData d = new DataBaseData();
        d = d.generateRandomBase(d);
        d.ontologies.add(Ontology.generateRandom());
        d.ontologies.add(Ontology.generateRandom());
        d.rules.add(RuleSet.generateRandom());
        d.rules.add(RuleSet.generateRandom());
        d.mediators.add(Mediator.generateRandom());
        d.mediators.add(Mediator.generateRandom());
        d.ports.add(Port.generateRandom());
        d.friends.add(Friend.generateRandom());
        return d;
    }

    public int getCounter()
    {
        return counter;
    }

    public void setCounter(int counter)
    {
        this.counter = counter;
    }

    public List<Ontology> getOntologies()
    {
        return ontologies;
    }

    public void setOntologies(List<Ontology> ontologies)
    {
        this.ontologies = ontologies;
    }

    public List<RuleSet> getRules()
    {
        return rules;
    }

    public List<Port> getPorts()
    {
        return ports;
    }

    public List<Port> getPorts(Set<Port.PortType> types)
    {
        List<Port> result = new ArrayList<Port>();
        for (Port port : ports) {
        	if (types.contains(port.getType()))
        		result.add(port);
		}
        return result;
    }

    public List<Mediator> getMediators()
    {
        return mediators;
    }

    public List<Friend> getFriends()
    {
    	return friends;
    }

    public void setRules(List<RuleSet> rules)
    {
        this.rules = rules;
    }

    public void setPorts(List<Port> ports)
    {
        this.ports = ports;
    }

    public void setMediators(List<Mediator> mediators)
    {
        this.mediators = mediators;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
//        result = prime * result + ((configs == null) ? 0 : configs.hashCode());
        result = prime * result + counter;
        result = prime * result + ((mediators == null) ? 0 : mediators.hashCode());
        result = prime * result + ((ontologies == null) ? 0 : ontologies.hashCode());
        result = prime * result + ((ports == null) ? 0 : ports.hashCode());
        result = prime * result + ((rules == null) ? 0 : rules.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        DataBaseData other = (DataBaseData) obj;
//        if (configs == null)
//        {
//            if (other.configs != null) return false;
//        }
//        else
//            if (!configs.equals(other.configs)) return false;
        if (counter != other.counter) return false;
        if (mediators == null)
        {
            if (other.mediators != null) return false;
        }
        else
            if (!mediators.equals(other.mediators)) return false;
        if (ontologies == null)
        {
            if (other.ontologies != null) return false;
        }
        else
            if (!ontologies.equals(other.ontologies)) return false;
        if (ports == null)
        {
            if (other.ports != null) return false;
        }
        else
            if (!ports.equals(other.ports)) return false;
        if (rules == null)
        {
            if (other.rules != null) return false;
        }
        else
            if (!rules.equals(other.rules)) return false;
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        builder.append(" {\n\tcounter: ");
        builder.append(counter);
        builder.append("\n\tontologies: ");
        builder.append(ontologies);
        builder.append("\n\tports: ");
        builder.append(ports);
        builder.append("\n\trules: ");
        builder.append(rules);
//        builder.append("\n\tconfigs: ");
//        builder.append(configs);
        builder.append("\n\tmediators: ");
        builder.append(mediators);
        builder.append("\n}");
        return builder.toString();
    }

    public String toJson()
    {
        return U.getGson().toJson(this);
    }

}

