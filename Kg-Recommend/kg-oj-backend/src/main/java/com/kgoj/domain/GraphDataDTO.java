package com.kgoj.domain;

import java.util.List;

public class GraphDataDTO {
    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;

    public List<NodeDTO> getNodes() { return nodes; }
    public void setNodes(List<NodeDTO> nodes) { this.nodes = nodes; }
    public List<EdgeDTO> getEdges() { return edges; }
    public void setEdges(List<EdgeDTO> edges) { this.edges = edges; }

    public static class NodeDTO {
        private String id;
        private String label;
        private String type; // 'Concept', 'KnowledgePoint', 'Exercise', 等

        public NodeDTO(String id, String label, String type) {
            this.id = id; this.label = label; this.type = type;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class EdgeDTO {
        private String source;
        private String target;
        private String label;

        public EdgeDTO(String source, String target, String label) {
            this.source = source; this.target = target; this.label = label;
        }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}