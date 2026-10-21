package blackout.service;
import blackout.structures.Graph;
import java.util.*;
public class FailureAnalysisService {
 private final Graph network=new Graph();
 public Graph getNetwork(){return network;}
 public List<Integer> affectedServers(int failedServerId){return network.bfs(failedServerId);}
 public boolean areConnected(int a,int b){return network.reachable(a).contains(b);}
}