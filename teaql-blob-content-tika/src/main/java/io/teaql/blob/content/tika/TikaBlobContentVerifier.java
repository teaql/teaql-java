package io.teaql.blob.content.tika;

import io.teaql.core.blob.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.tika.detect.CompositeDetector;
import org.apache.tika.detect.microsoft.POIFSContainerDetector;
import org.apache.tika.detect.microsoft.ooxml.OPCPackageDetector;
import org.apache.tika.detect.zip.DefaultZipContainerDetector;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.TIFF;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.parser.image.JpegParser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.parser.mp4.MP4Parser;
import org.apache.tika.parser.pdf.*;
import org.apache.tika.parser.pages.TextPolicy;
import static io.teaql.core.blob.BlobException.Code.*;

/** Explicit parsers only: no AutoDetectParser, filename hints, OCR or embedded-document recursion. */
public final class TikaBlobContentVerifier implements BlobContentVerifier {
    public record Limits(long maximumExpandedBytes, int maximumPackageEntries, long maximumTextCharacters, int concurrentVerifications) {
        public Limits {
            if (maximumExpandedBytes<=0 || maximumPackageEntries<=0 || maximumTextCharacters<=0
                    || concurrentVerifications<=0 || concurrentVerifications>64) throw new BlobException(BLOB_POLICY_INVALID);
        }
        public static Limits defaults() { return new Limits(104857600,10000,1048576,2); }
    }
    private final Path temporaryRoot;
    private final Limits limits;
    private final Semaphore permits;
    /** Passive assembly. Private staging directories are created only during verification. */
    public TikaBlobContentVerifier(Path temporaryRoot,Limits limits) {
        this.temporaryRoot=Objects.requireNonNull(temporaryRoot).toAbsolutePath().normalize(); this.limits=Objects.requireNonNull(limits);
        permits=new Semaphore(limits.concurrentVerifications());
    }

    @Override public Verified verify(InputStream source,String extension,long maximumBytes,boolean allowEmpty) {
        Objects.requireNonNull(source);
        if (maximumBytes<=0) throw new BlobException(BLOB_POLICY_INVALID);
        if (extension==null || !Set.of("jpg","jpeg","png","mp3","mp4","pdf","doc","docx","xls","xlsx").contains(extension)) throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
        String expected=BlobFieldSpec.of("Blob","content",List.of(extension),true,"default").allowedContentTypes().first();
        if (!permits.tryAcquire()) throw new BlobException(BLOB_VERIFICATION_BUSY);
        Path temporary=null;
        try {
            temporary=createStage();
            long size=0; MessageDigest digest=MessageDigest.getInstance("SHA-256"); byte[] buffer=new byte[65536];
            try (OutputStream output=Files.newOutputStream(temporary)) {
                while (true) {
                    long remaining=maximumBytes-size; int count=source.read(buffer,0,(int)Math.min(buffer.length,remaining==Long.MAX_VALUE?buffer.length:remaining+1));
                    if (count<0) break;
                    if (count==0) { int value=source.read(); if (value<0) break; buffer[0]=(byte)value; count=1; }
                    if (count>remaining) throw new BlobException(BLOB_SIZE_EXCEEDED);
                    output.write(buffer,0,count); digest.update(buffer,0,count); size+=count;
                }
            } catch (IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
            if (size==0 && !allowEmpty) throw new BlobException(BLOB_SIZE_EXCEEDED);
            preflightPackage(temporary);
            ParseContext context=context(); Metadata metadata=new Metadata();
            MimeTypes mimeTypes=MimeTypes.getDefaultMimeTypes();
            var detector=new CompositeDetector(mimeTypes.getMediaTypeRegistry(),List.of(mimeTypes,new POIFSContainerDetector(),
                    new DefaultZipContainerDetector(List.of(new OPCPackageDetector()))));
            String actual;
            try (TemporaryResources resources=temporaryResources(); TikaInputStream input=TikaInputStream.get(temporary,metadata,resources)) {
                actual=mimeTypes.getMediaTypeRegistry().normalize(detector.detect(input,metadata,context)).toString();
            }
            boolean mp4=extension.equals("mp4");
            if (!expected.equals(actual) && !(mp4 && Set.of("video/quicktime","application/mp4").contains(actual))) {
                throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
            }
            Parser parser=parser(extension);
            Metadata parsed=new Metadata();
            try (TemporaryResources resources=temporaryResources(); TikaInputStream input=TikaInputStream.get(temporary,parsed,resources)) {
                parser.parse(input,new TextBudget(limits.maximumTextCharacters()),parsed,context);
            }
            if (mp4) {
                actual=parsed.get(HttpHeaders.CONTENT_TYPE);
                if (!expected.equals(actual) || !positive(parsed.get(TIFF.IMAGE_WIDTH)) || !positive(parsed.get(TIFF.IMAGE_LENGTH))) {
                    throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
                }
            }
            return new Verified(actual,size,HexFormat.of().formatHex(digest.digest()));
        } catch (BlobException known) { throw known; }
        catch (Exception invalid) { throw new BlobException(BLOB_FORMAT_NOT_ALLOWED); }
        finally {
            try { if (temporary!=null) Files.deleteIfExists(temporary); }
            catch (IOException cleanupFailed) { throw new BlobException(BLOB_STORE_FAILED); }
            finally { permits.release(); }
        }
    }

    private Path createStage() {
        try {
            Files.createDirectories(temporaryRoot,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            if (!Files.isDirectory(temporaryRoot,LinkOption.NOFOLLOW_LINKS)
                    || Files.getPosixFilePermissions(temporaryRoot,LinkOption.NOFOLLOW_LINKS).stream().anyMatch(p -> p.name().startsWith("GROUP_") || p.name().startsWith("OTHERS_"))) {
                throw new BlobException(BLOB_POLICY_INVALID);
            }
            return Files.createTempFile(temporaryRoot,"verify-",".bin");
        } catch (IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
        catch (UnsupportedOperationException unsupported) { throw new BlobException(BLOB_POLICY_INVALID); }
    }

    private TemporaryResources temporaryResources() {
        TemporaryResources resources=new TemporaryResources(); resources.setTemporaryFileDirectory(temporaryRoot); return resources;
    }

    private static boolean positive(String value) {
        if (value==null) return false;
        try { return Long.parseLong(value)>0; }
        catch (NumberFormatException invalid) { return false; }
    }

    private ParseContext context() throws Exception {
        var context=new ParseContext();
        context.set(SAXParser.class,secureXml());
        context.set(EmbeddedDocumentExtractor.class,new NoEmbeddedDocuments());
        PDFParserConfig pdf=new PDFParserConfig(); pdf.getPages().setText(TextPolicy.EXTRACT);
        pdf.setExtractInlineImages(false); pdf.setExtractInlineImageMetadataOnly(false); pdf.setExtractActions(false);
        pdf.setExtractAcroFormContent(false); pdf.setMaxMainMemoryBytes(8*1024*1024); pdf.setMaxPages(10000); context.set(PDFParserConfig.class,pdf);
        return context;
    }
    private static Parser parser(String extension) {
        OfficeParserConfig office=new OfficeParserConfig(); office.setExtractMacros(false); office.setExtractThumbnail(false);
        return switch (extension) {
            case "jpg","jpeg" -> new JpegParser();
            case "png" -> new ImageParser();
            case "pdf" -> new PDFParser();
            case "doc","xls" -> new OfficeParser(office);
            case "docx","xlsx" -> new OOXMLParser(office);
            case "mp3" -> new Mp3Parser();
            case "mp4" -> new MP4Parser();
            default -> throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
        };
    }
    private void preflightPackage(Path path) throws Exception {
        try (var source=Files.newInputStream(path)) {
            byte[] magic=source.readNBytes(4);
            if (magic.length!=4 || magic[0]!='P' || magic[1]!='K') return;
        }
        long[] expanded={0}; Set<String> names=new HashSet<>(); int entries=0;
        try (var zip=new ZipInputStream(Files.newInputStream(path))) {
            java.util.zip.ZipEntry entry;
            while ((entry=zip.getNextEntry())!=null) {
                if (++entries>limits.maximumPackageEntries()) throw new BlobException(BLOB_SIZE_EXCEEDED);
                String name=entry.getName();
                if (name.indexOf('\0')>=0 || name.startsWith("/") || name.indexOf('\\')>=0 || Arrays.asList(name.split("/",-1)).contains("..") || !names.add(name)) {
                    throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
                }
                if (entry.getSize()>limits.maximumExpandedBytes()-expanded[0]) throw new BlobException(BLOB_SIZE_EXCEEDED);
                InputStream bounded=new ExpandedInput(zip,expanded,limits.maximumExpandedBytes());
                if (!entry.isDirectory() && (name.endsWith(".xml") || name.endsWith(".rels"))) secureXml().parse(bounded,new TextBudget(limits.maximumTextCharacters()));
                else { byte[] buffer=new byte[65536]; while (bounded.read(buffer)>=0) { } }
                zip.closeEntry();
            }
        }
        if (entries==0) throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
    }
    private static SAXParser secureXml() throws Exception {
        SAXParserFactory factory=SAXParserFactory.newDefaultInstance(); factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
        SAXParser parser=factory.newSAXParser(); parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,""); parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,""); return parser;
    }
    private static final class ExpandedInput extends FilterInputStream {
        private final long[] total; private final long maximum;
        ExpandedInput(InputStream source,long[] total,long maximum) { super(source); this.total=total; this.maximum=maximum; }
        @Override public int read() throws IOException { int value=in.read(); if (value>=0) count(1); return value; }
        @Override public int read(byte[] bytes,int offset,int length) throws IOException {
            long remaining=maximum-total[0];
            int n=in.read(bytes,offset,(int)Math.min(length,remaining==Long.MAX_VALUE?length:remaining+1)); if (n>0) count(n); return n;
        }
        private void count(int n) { if (n>maximum-total[0]) throw new BlobException(BLOB_SIZE_EXCEEDED); total[0]+=n; }
        @Override public void close() { /* The enclosing package reader owns the ZIP stream. */ }
    }
    private static final class TextBudget extends DefaultHandler {
        private long remaining;
        TextBudget(long maximum) { remaining=maximum; }
        @Override public void characters(char[] data,int start,int length) { if (length>remaining) throw new BlobException(BLOB_SIZE_EXCEEDED); remaining-=length; }
    }
    private static final class NoEmbeddedDocuments implements EmbeddedDocumentExtractor {
        @Override public boolean shouldParseEmbedded(Metadata metadata,ParseContext context) { return false; }
        @Override public void parseEmbedded(TikaInputStream source,ContentHandler handler,Metadata metadata,ParseContext context,boolean outputHtml) { }
    }
}
