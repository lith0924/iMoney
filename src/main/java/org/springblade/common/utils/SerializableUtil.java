package org.springblade.common.utils;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableWebMvc
public class SerializableUtil {
	@Bean
	public Jackson2ObjectMapperBuilderCustomizer builderCustomizer() {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter dateTimeSerializeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		DateTimeFormatter dateTimeDeserializeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		return builder -> {
			//反序列化
			// 所有Long类型转换成String到前台
			builder.serializerByType(Long.class, ToStringSerializer.instance);
			builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
			builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeSerializeFormatter));
			builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeDeserializeFormatter));
		};
	}
}
