package org.opennms.arnet.model

data class Topology(
    val vertices: Set<Vertex>? = null,
    val edges: Set<Edge>? = null,
    val alarms: Set<Alarm>? = null,
    val situations: Set<Situation>? = null
)