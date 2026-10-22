package a311.college.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.File;

import java.util.List;


/**
 * 共享 PDF 库文件服务
 * <p>
 * 服务启动时自动从 pdfs/ 目录扫描所有 PDF 文件，加载到内存向量库。
 * 服务关闭后向量库自动清空，所有聊天会话共享同一个向量库。
 * </p>
 */
@Slf4j
@Service
public class FileServiceImpl {

    /**
     * PDF 库存放目录
     * <p>
     * 通过环境变量 COLLEGE_PDF_DIR 配置，默认为运行目录下的 pdf/；
     * 目录不存在时仅告警并跳过（RAG 知识库为可选能力，不应阻断启动）。
     * </p>
     */
    @Value("${college.pdf.dir:pdf}")
    private String pdfDir;

    private final VectorStore vectorStore;

    @Autowired
    public FileServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 服务启动时执行：扫描 pdfs/ 目录，将所有 PDF 文件加载到内存向量库
     */
    @PostConstruct
    public void init() {
        loadAllPdfsFromDisk();
    }

    /**
     * 扫描 pdfs/ 目录，将所有 PDF 文件加载到向量库
     */
    private void loadAllPdfsFromDisk() {
        File dir = new File(pdfDir);
        if (!dir.exists()) {
            log.warn("RAG 知识库目录不存在，跳过 PDF 导入：{}", pdfDir);
            return;
        }
        if (!dir.isDirectory()) {
            log.warn("RAG 知识库路径不是目录，跳过 PDF 导入：{}", pdfDir);
            return;
        }
        File[] pdfFiles = dir.listFiles((document, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            log.info("RAG 知识库目录下没有 PDF 文件：{}", pdfDir);
            return;
        }
        for (File pdfFile : pdfFiles) {
            try {
                loadPdfToVectorStore(pdfFile);
                log.info("导入文件{}到向量数据库成功", pdfFile.getName());
            } catch (Exception e) {
                log.error("导入文件{}到向量数据库失败", pdfFile.getName(), e);
            }
        }
    }

    /**
     * 加载单个 PDF 文件到向量库
     *
     * @param pdfFile PDF 文件
     */
    private void loadPdfToVectorStore(File pdfFile) {
        FileSystemResource resource = new FileSystemResource(pdfFile);
        // 创建 PDF 读取器，每页作为一个 Document
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 按照页数拆分pdf，每一页作为一个Document
                        .withPageTopMargin(30) // 去掉顶部30px（页眉）
                        .withPageBottomMargin(30) // 去掉底部30px（页脚）
                        .build()
        );
        // 读取并拆分文档
        List<Document> documents = reader.read();
        documents.forEach(document -> document.getMetadata().put("source_file", pdfFile.getName()));
        // 写入向量库
        vectorStore.add(documents);
    }

}