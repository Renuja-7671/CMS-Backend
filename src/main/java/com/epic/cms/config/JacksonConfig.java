package com.epic.cms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson configuration for proper date/time serialization.
 * Configures ObjectMapper to handle Java 8 date/time types (LocalDate, LocalDateTime, etc.).
 */
@Configuration
public class JacksonConfig {

	/**
	 * Configure global ObjectMapper with JavaTimeModule for date/time serialization.
	 * This ensures that LocalDate and LocalDateTime are serialized as ISO-8601 strings
	 * and properly deserialized back to Java objects.
	 * 
	 * @return Configured ObjectMapper instance
	 */
	@Bean
	@Primary
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		
		// Register JavaTimeModule to handle Java 8 date/time types
		mapper.registerModule(new JavaTimeModule());
		
		// Disable writing dates as timestamps (use ISO-8601 strings instead)
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		
		return mapper;
	}
}
