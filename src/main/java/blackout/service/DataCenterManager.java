package blackout.service;
import blackout.model.*;
import blackout.structures.*;
public class DataCenterManager {
    private final CustomLinkedList<Server> servers=new CustomLinkedList<>();
    private final CustomHashTable<Integer,Server> byId=new CustomHashTable<>();
    private final CustomQueue<Job> jobs=new CustomQueue<>();
    private final CustomStack<String> configurationHistory=new CustomStack<>();
    public void registerServer(Server s){if(byId.containsKey(s.getId()))throw new IllegalArgumentException("Duplicate server id");servers.add(s);byId.put(s.getId(),s);}
    public Server findServer(int id){return byId.get(id);}
    public void submitJob(Job job){jobs.enqueue(job);}
    public Job processNextJob(){if(jobs.isEmpty())return null;Job j=jobs.dequeue();j.setCompleted(true);return j;}
    public void saveConfiguration(String snapshot){configurationHistory.push(snapshot);}
    public String rollbackConfiguration(){return configurationHistory.isEmpty()?null:configurationHistory.pop();}
    public int serverCount(){return servers.size();}
    public int queuedJobs(){return jobs.size();}
}