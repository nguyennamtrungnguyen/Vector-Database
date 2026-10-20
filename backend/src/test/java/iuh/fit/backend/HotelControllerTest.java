package iuh.fit.backend;

import iuh.fit.backend.dto.HotelDTO;
import iuh.fit.backend.dto.SearchResponseDTO;
import iuh.fit.backend.dto.SearchResultDTO;
import iuh.fit.backend.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test Controller với MockMvc và mock Service (interface).
 */
@WebMvcTest(controllers = {iuh.fit.backend.controller.HotelController.class,
        iuh.fit.backend.controller.GlobalExceptionHandler.class})
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HotelService hotelService;

    private HotelDTO sampleHotel;

    @BeforeEach
    void setUp() {
        sampleHotel = new HotelDTO("H001", "Pine Valley Resort", "Đà Lạt",
                1200000L, 4, "Không gian thanh bình.", List.of("wifi", "spa"));
    }

    @Test
    void getAllHotels_shouldReturnList() throws Exception {
        when(hotelService.getAllHotels()).thenReturn(List.of(sampleHotel));

        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("H001"))
                .andExpect(jsonPath("$[0].name").value("Pine Valley Resort"));
    }

    @Test
    void search_keyword_shouldReturnResults() throws Exception {
        var response = new SearchResponseDTO("keyword", 5L,
                List.of(new SearchResultDTO(sampleHotel, null)));
        when(hotelService.keywordSearch(anyString(), anyInt(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/hotels/search")
                        .param("q", "rừng thông")
                        .param("mode", "keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("keyword"))
                .andExpect(jsonPath("$.results[0].hotel.id").value("H001"))
                .andExpect(jsonPath("$.results[0].score").doesNotExist());
    }

    @Test
    void search_semantic_shouldReturnResultsWithScore() throws Exception {
        var response = new SearchResponseDTO("semantic", 120L,
                List.of(new SearchResultDTO(sampleHotel, 0.87)));
        when(hotelService.semanticSearch(anyString(), anyInt(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/hotels/search")
                        .param("q", "chỗ nghỉ yên tĩnh giữa núi rừng")
                        .param("mode", "semantic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("semantic"))
                .andExpect(jsonPath("$.results[0].score").value(0.87));
    }

    @Test
    void search_missingQuery_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/hotels/search")
                        .param("mode", "semantic"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void search_invalidMode_shouldReturn400() throws Exception {
        when(hotelService.keywordSearch(any(), anyInt(), any(), any()))
                .thenThrow(new IllegalArgumentException("Tham số 'mode' phải là 'keyword' hoặc 'semantic'"));

        mockMvc.perform(get("/api/hotels/search")
                        .param("q", "test")
                        .param("mode", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void findSimilar_notFound_shouldReturn404() throws Exception {
        when(hotelService.findSimilar(eq("NOTEXIST"), anyInt()))
                .thenThrow(new iuh.fit.backend.exception.HotelNotFoundException("NOTEXIST"));

        mockMvc.perform(get("/api/hotels/NOTEXIST/similar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
