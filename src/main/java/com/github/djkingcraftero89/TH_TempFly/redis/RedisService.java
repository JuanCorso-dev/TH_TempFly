package com.github.djkingcraftero89.TH_TempFly.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

import java.util.function.BiConsumer;

public class RedisService {
	private final boolean enabled;
	private final String channel;
	private RedisClient client;
	private StatefulRedisConnection<String, String> connection;
	private StatefulRedisPubSubConnection<String, String> pubSubConnection;

	public RedisService(boolean enabled) {
		this.enabled = enabled;
		this.channel = null;
	}

	public RedisService(boolean enabled, String host, int port, String username, String password, int database, String clientName, String channel) {
		this.enabled = enabled;
		this.channel = channel;
		if (!enabled) return;
		RedisURI.Builder builder = RedisURI.builder().withHost(host).withPort(port).withDatabase(database);
		if (username != null && !username.isEmpty()) builder.withAuthentication(username, password == null ? "" : password);
		else if (password != null && !password.isEmpty()) builder.withPassword(password.toCharArray());
		if (clientName != null && !clientName.isEmpty()) builder.withClientName(clientName);
		RedisURI uri = builder.build();
		this.client = RedisClient.create(uri);
		this.connection = client.connect();
	}

	public void subscribe(BiConsumer<String, String> handler) {
		if (!enabled) return;
		this.pubSubConnection = client.connectPubSub();
		this.pubSubConnection.sync().subscribe(channel);
		this.pubSubConnection.addListener(new io.lettuce.core.pubsub.RedisPubSubAdapter<>() {
			@Override
			public void message(String channel, String message) {
				handler.accept(channel, message);
			}
		});
	}

	public void publish(String message) {
		if (!enabled) return;
		RedisCommands<String, String> cmd = connection.sync();
		cmd.publish(channel, message);
	}

	public void close() {
		if (!enabled) return;
		if (pubSubConnection != null) pubSubConnection.close();
		if (connection != null) connection.close();
		if (client != null) client.shutdown();
	}
}
