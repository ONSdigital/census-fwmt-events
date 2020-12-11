package uk.gov.ons.census.fwmt.events.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class GatewayRabbitConfig {

  public static final String GATEWAY_EVENTS_ROUTING_KEY = "Gateway.Event";
  public static final String GATEWAY_EVENTS_EXCHANGE = "Gateway.Events.Exchange";

  @Bean
  public DirectExchange eventExchange() {
    return new DirectExchange(GATEWAY_EVENTS_EXCHANGE);
  }

  @Bean(name = "gatewayConnectionFactory")
  @Primary
  public CachingConnectionFactory gatewayConnectionFactory(
      @Value("${app.rabbitmq.gw.host}") String host,
      @Value("${app.rabbitmq.gw.port}") int port,
      @Value("${app.rabbitmq.gw.username}") String username,
      @Value("${app.rabbitmq.gw.password}") String password,
      @Value("${app.rabbitmq.gw.virtualHost}") String virtualHost) {

    log.info("Creating gatewayConnectionFactory with host: {}, on port {}", host, port);

    final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
    connectionFactory.setHost(host);
    connectionFactory.setPort(port);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);
    connectionFactory.setVirtualHost(virtualHost);

    return connectionFactory;
  }

  @Bean("GW_EVENT_MC")
  public MessageConverter jsonMessageConverter(@Qualifier("GW_EVENT_CM") DefaultClassMapper classMapper) {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    Jackson2JsonMessageConverter jsonMessageConverter = new Jackson2JsonMessageConverter(objectMapper);
    jsonMessageConverter.setClassMapper(classMapper);
    return jsonMessageConverter;
  }

  @Bean("GW_EVENT_RT")
  public RabbitTemplate rabbitTemplate(@Qualifier("gatewayConnectionFactory") ConnectionFactory gatewayConnectionFactory,
      @Qualifier("GW_EVENT_MC") MessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(gatewayConnectionFactory);
    template.setMessageConverter(messageConverter);
    return template;
  }

  @Bean("GW_EVENT_CM")
  public DefaultClassMapper classMapper() {
    DefaultClassMapper classMapper = new DefaultClassMapper();
    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put("uk.gov.ons.census.fwmt.events.data.GatewayEventDTO", GatewayEventDTO.class);
    idClassMapping.put("uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO", GatewayErrorEventDTO.class);
    classMapper.setIdClassMapping(idClassMapping);
    return classMapper;
  }

  @Bean
  @Qualifier("GW_errorE")
  public DirectExchange gwErrorExchange() {
    DirectExchange directExchange = new DirectExchange("GW.Error.Exchange");
    return directExchange;
  }

  @Bean
  @Qualifier("GW_errorQ")
  public Queue gwErrorQ() {
    Queue queue = QueueBuilder.durable("GW.ErrorQ")
        .withArgument("GW.Error.Exchange", "")
        .withArgument("gw.receiver.error", "GW.ErrorQ")
        .build();
    return queue;
  }

}
