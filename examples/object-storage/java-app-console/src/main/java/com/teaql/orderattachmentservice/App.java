package com.teaql.orderattachmentservice;

import com.teaql.orderattachmentservice.orderattachment.OrderAttachment;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.teaql.core.DataServiceExecutor;
import io.teaql.core.DataServiceRegistry;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.storage.ObjectStorage.Key;
import io.teaql.core.storage.ObjectStorage.PutOptions;
import io.teaql.core.storage.ObjectStorage.WriteMode;
import io.teaql.core.storage.ObjectStorageObserver;
import io.teaql.core.storage.ObjectStorageProvider;
import io.teaql.core.storage.StorageException;
import io.teaql.objectstorage.s3.S3ObjectStorage;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.AppAuditEventSink;
import io.teaql.runtime.SafeAuditEvent;
import io.teaql.runtime.TeaQLRuntime;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;

public class App {
  private static final byte[] RECEIPT = "TeaQL order receipt fixture\nItem: tea\nTotal: 12.50\n".getBytes(StandardCharsets.UTF_8);
  private static final String FILENAME = "receipt.txt";
  private static final String CONTENT_TYPE = "text/plain";

  public static void main(String[] args) throws Exception {
    int round = Integer.parseInt(required("TEAQL_ATTACHMENT_ROUND"));
    require(round == 1 || round == 2, "round must be 1 or 2");
    var config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + required("TEAQL_ATTACHMENT_DB"));
    config.setMaximumPoolSize(1);
    var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(
        required("TEAQL_S3_ACCESS_KEY"), required("TEAQL_S3_SECRET_KEY")));
    try (var source = new HikariDataSource(config);
         var provider = S3ObjectStorage.connect(URI.create(required("TEAQL_S3_ENDPOINT")), Region.US_EAST_1,
             credentials, required("TEAQL_S3_BUCKET"), true)) {
      var app = new App();
      var executor = new JdbcSqlExecutor(source);
      try (var runtime = app.teaQLRuntime(app.entityMetaFactory(),
          app.dataServiceRegistry(app.dataServiceExecutor(executor, source)),
          app.teaQLDatabase(executor, source))) {
        var context = new CustomUserContext(runtime);
        verify(context, provider, Path.of(required("TEAQL_ATTACHMENT_DOWNLOAD")), round);
      }
    }
  }

  private static void verify(UserContext context, ObjectStorageProvider provider, Path download, int round) throws Exception {
    var events = new ArrayList<String>();
    var audits = new ArrayList<SafeAuditEvent>();
    context.putAttribute(ObjectStorageProvider.class.getName(), provider);
    context.putAttribute(ObjectStorageObserver.class.getName(), (ObjectStorageObserver) (caller, operation, intent) -> {
      require(caller == context, "storage observation escaped the attachment Context");
      return outcome -> events.add(operation + "|" + intent + "|" + outcome);
    });
    context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> {
      if (List.of("CustomerOrder", "OrderAttachment").contains(event.entityType())) audits.add(event);
    });
    var storage = context.objectStorage("file and retrieve an order receipt");
    context.ensureSchema();

    var platform = Q.commercePlatforms().withIdIs(1L).limit(1).comment("load the commerce platform")
        .purpose("associate the new order to its root").executeForList(context).get(0);
    String orderNumber = "ATTACH-ORDER-" + round;
    var order = Q.customerOrders().comment("prepare receipt order").purpose("file a receipt against this order").newEntity(context);
    order.updateOrderNumber(orderNumber);
    order.updateCommercePlatform(platform);
    order.auditAs("place the order receiving the receipt").save(context);

    var key = new Key("private-order-attachments/" + UUID.randomUUID());
    var digest = checksum(RECEIPT);
    boolean uploaded = false;
    try {
      require(!storage.exists(key), "new attachment key already exists");
      storage.putBytes(key, RECEIPT, new PutOptions(RECEIPT.length, CONTENT_TYPE, Map.of("receipt-sha256", digest), WriteMode.CREATE_ONLY));
      uploaded = true;
      var attachment = Q.orderAttachments().comment("prepare receipt attachment")
          .purpose("save the uploaded receipt reference on its order").newEntity(context);
      attachment.updateCustomerOrder(order);
      attachment.updateFileName(FILENAME);
      attachment.updateContentType(CONTENT_TYPE);
      attachment.updateContentLength(RECEIPT.length);
      attachment.updateContentSha256(digest);
      attachment.updateObjectKey(key.value());
      attachment.auditAs("associate the stored receipt with its order").save(context);

      var rows = attachments(context, order.getId());
      require(rows.size() == 1 && Objects.equals(rows.get(0).getId(), attachment.getId()), "persisted order association mismatch");
      var expression = E.orderAttachment(rows.get(0));
      require(FILENAME.equals(expression.getFileName().eval()) && CONTENT_TYPE.equals(expression.getContentType().eval()),
          "stored receipt filename or type mismatch");
      require(Objects.equals(expression.getContentLength().eval(), RECEIPT.length)
          && digest.equals(expression.getContentSha256().eval()), "stored receipt bytes metadata mismatch");
      var reference = expression.getObjectKey().eval();
      require(key.value().equals(reference), "stored receipt reference mismatch");
      var persistedKey = new Key(reference);
      var metadata = storage.head(persistedKey).orElseThrow();
      require(metadata.size() == RECEIPT.length && CONTENT_TYPE.equals(metadata.contentType())
          && digest.equals(metadata.customMetadata().get("receipt-sha256")), "provider metadata mismatch");
      byte[] replacement = "replacement".getBytes(StandardCharsets.UTF_8);
      expect(StorageException.Code.CONFLICT, () -> storage.putBytes(persistedKey, replacement, PutOptions.create(replacement.length)));
      expect(StorageException.Code.TOO_LARGE, () -> storage.getBytes(persistedKey, RECEIPT.length - 1));
      byte[] downloaded = storage.getBytes(persistedKey, RECEIPT.length);
      require(Arrays.equals(downloaded, RECEIPT) && checksum(downloaded).equals(digest), "retrieved receipt changed");
      Files.write(download, downloaded);

      storage.delete(persistedKey);
      uploaded = false;
      expect(StorageException.Code.NOT_FOUND, () -> storage.get(persistedKey));
      require(!storage.exists(persistedKey), "missing object reported present");
      require(attachments(context, order.getId()).stream().map(OrderAttachment::getId).toList().equals(List.of(attachment.getId())),
          "missing object changed the order association");
      require(!events.isEmpty() && audits.stream().map(SafeAuditEvent::entityType).toList().equals(List.of("CustomerOrder", "OrderAttachment")),
          "committed order and attachment audits missing");
      require(!events.toString().contains(key.value()) && !audits.toString().contains(key.value()), "private object key reached safe events");

      System.out.printf("ATTACHMENT_ORIGIN %s%n", OrderAttachment.class.getProtectionDomain().getCodeSource().getLocation());
      System.out.printf("S3_PROVIDER_ORIGIN %s%n", S3ObjectStorage.class.getProtectionDomain().getCodeSource().getLocation());
      System.out.printf("ORDER_ATTACHMENT {\"round\":%d,\"order\":%d,\"attachment\":%d,\"orderNumber\":\"%s\",\"filename\":\"%s\",\"contentType\":\"%s\","
              + "\"length\":%d,\"sha256\":\"%s\",\"conflict\":true,\"sizeRefused\":true,\"missing\":true,\"businessAudits\":%d}%n",
          round, order.getId(), attachment.getId(), orderNumber, FILENAME, CONTENT_TYPE, downloaded.length, digest, audits.size());
    } finally {
      if (uploaded) storage.delete(key);
    }
  }

  private static List<OrderAttachment> attachments(UserContext context, Long orderId) {
    return Q.orderAttachments().filterByCustomerOrder(orderId).limit(2).comment("load order attachments")
        .purpose("retrieve this order's stored receipt").executeForList(context).stream().toList();
  }

  private static String checksum(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  private static String required(String name) {
    var value = System.getenv(name);
    if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + name);
    return value;
  }

  private static void require(boolean ok, String reason) {
    if (!ok) throw new AssertionError(reason);
  }

  private static void expect(StorageException.Code code, Runnable operation) {
    try { operation.run(); }
    catch (StorageException error) { require(error.code() == code, "unexpected storage error"); return; }
    throw new AssertionError("expected typed storage error");
  }

  public EntityMetaFactory entityMetaFactory() {
    SimpleEntityMetaFactory factory = new SimpleEntityMetaFactory();
    GeneratedRuntimeModule.module().install(factory);
    EntityMetaFactory.registerGlobal(factory);
    return factory;
  }

  public DataServiceExecutor dataServiceExecutor(JdbcSqlExecutor executor, DataSource dataSource) {
    return new io.teaql.core.sqlite.SqliteDataServiceExecutor("default", executor, dataSource);
  }

  public DataServiceRegistry dataServiceRegistry(DataServiceExecutor dataServiceExecutor) {
    var registry = new io.teaql.runtime.DefaultDataServiceRegistry();
    registry.register("default", dataServiceExecutor);
    return registry;
  }

  public WorkspaceTeaQLDatabase teaQLDatabase(JdbcSqlExecutor executor, DataSource source) {
    return new WorkspaceTeaQLDatabase(executor, source);
  }

  public TeaQLRuntime teaQLRuntime(EntityMetaFactory metadata, DataServiceRegistry registry, WorkspaceTeaQLDatabase database) {
    return TeaQLRuntime.builder().metadata(metadata).registry(registry)
        .idGenerationService(new IdSpaceIdGenerator(database))
        .queryExecutionLogging(false).mutationExecutionLogging(false)
        .build().install(GeneratedRuntimeModule.module());
  }
}
