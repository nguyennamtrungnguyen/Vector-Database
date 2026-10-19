package iuh.fit.backend;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.ai.google.genai.embedding.api-key=test-key")
class BackendApplicationTests {

	@MockitoBean
	private EmbeddingModel embeddingModel;

	@MockitoBean
	private SimpleVectorStore simpleVectorStore;

	@Test
	void contextLoads() {
	}

}
