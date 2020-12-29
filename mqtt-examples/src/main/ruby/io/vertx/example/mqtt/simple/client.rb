require 'vertx-mqtt/mqtt_client'
require 'vertx/buffer'
@MQTT_MESSAGE = "Hello Vert.x MQTT Client"
@BROKER_HOST = "localhost"
@BROKER_PORT = 1883
@MQTT_TOPIC = "/my_topic"
mqttClient = VertxMqtt::MqttClient.create($vertx)

mqttClient.connect(@BROKER_PORT, @BROKER_HOST) { |ch_err,ch|
  if (ch_err == nil)
    puts "Connected to a server"

    mqttClient.publish(@MQTT_TOPIC, Vertx::Buffer.buffer(@MQTT_MESSAGE), :AT_MOST_ONCE, false, false) { |s_err,s|
      mqttClient.disconnect() { |d_err,d|
        puts "Disconnected from server"
      }
    }
  else
    puts "Failed to connect to a server"
    puts ch_err
  end
}
