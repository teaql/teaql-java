module io.teaql.redis {
    requires io.teaql.utils;
    requires redis.clients.jedis;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;

    requires io.teaql.core;

    exports io.teaql.redis;

    provides io.teaql.core.spi.RemoteCacheProvider with io.teaql.redis.RedisRemoteCacheProvider;
    provides io.teaql.core.spi.RemoteLockProvider with io.teaql.redis.RedisRemoteLockProvider;
}
