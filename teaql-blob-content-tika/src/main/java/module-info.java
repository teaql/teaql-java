module io.teaql.blob.content.tika {
    requires transitive io.teaql.blob.staging.file;
    requires transitive io.teaql.core;
    requires org.apache.tika.core;
    requires org.apache.tika.parser.microsoft;
    requires org.apache.tika.parser.zip;
    requires org.apache.tika.parser.image;
    requires org.apache.tika.parser.pdf;
    requires org.apache.tika.parser.audiovideo;
    requires java.xml;
    requires org.apache.pdfbox;
    requires org.apache.pdfbox.io;
    exports io.teaql.blob.content.tika;
}
