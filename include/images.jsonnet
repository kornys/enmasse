local version = std.extVar("VERSION");
local project = "enmasseproject";
{
  image(name)::
    project + "/" + name + ":" + version,
    
  address_controller::
    self.image("address-controller"),

  router::
    self.image("qdrouterd"),

  artemis::
    self.image("artemis"),

  topic_forwarder::
    self.image("topic-forwarder"),

  router_metrics::
    self.image("router-metrics"),

  configserv::
    self.image("configserv"),

  queue_scheduler::
    self.image("queue-scheduler"),

  ragent::
    self.image("ragent"),

  subserv::
    self.image("subserv"),

  console::
    self.image("console"),

  mqtt_gateway::
    self.image("mqtt-gateway"),

  mqtt_lwt::
    self.image("mqtt-lwt"),

  amqp_kafka_bridge::
    self.image("amqp-kafka-bridge")
}
