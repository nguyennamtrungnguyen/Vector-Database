# MEGO Hotel Search Demo 🏨 (Google Gemini Embedding)

Demo ứng dụng tìm kiếm khách sạn bằng **Vector Database** — so sánh trực tiếp tìm kiếm ngữ nghĩa (Vector Search) với tìm theo từ khóa (SQL LIKE), dùng **Gemini Embedding API** (`gemini-embedding-001`) qua Spring AI.

## Yêu cầu hệ thống

| Thành phần | Phiên bản |
|-----------|-----------|
| Java | 21 |
| Node.js | 20+ |
| npm | 9+ |
| Gemini API key | Tạo miễn phí tại Google AI Studio |

## 1. Chuẩn bị API key Gemini

### Bước 1: Tạo API key
- Truy cập [https://aistudio.google.com/apikey](https://aistudio.google.com/apikey), đăng nhập tài khoản Google và bấm **Create API key**.

### Bước 2: Khai báo key cho backend
Trong thư mục `backend`, copy file mẫu thành `.env` rồi điền key:
```powershell
cd backend
copy .env.example .env
```
Nội dung `backend/.env`:
```properties
GEMINI_API_KEY=AIza...key-cua-ban
```
> File `.env` đã nằm trong `.gitignore`, **không commit** key lên Git. Có thể thay bằng biến môi trường hệ thống: `$env:GEMINI_API_KEY="AIza..."` (PowerShell).

Model embedding sử dụng: `gemini-embedding-001` (768 chiều, hỗ trợ tiếng Việt) — cấu hình trong `application.properties`.

---

## 2. Cách chạy dự án

### Backend (Spring Boot)
1. Mở PowerShell tại thư mục `backend`:
```powershell
cd backend
.\gradlew.bat bootRun
```
*Lưu ý: Lần khởi động đầu tiên, `HotelDataLoader` sẽ gọi Gemini API để tạo vector embedding cho 25 khách sạn và lưu vào file `data/vector-store-gemini.json`. Các lần khởi động tiếp theo sẽ nạp trực tiếp từ file này nên khởi động cực nhanh và không tốn tài nguyên.*

- Backend API: http://localhost:8080  
- Swagger UI tài liệu API: http://localhost:8080/swagger-ui.html

### Frontend (React + Vite + Tailwind)
1. Mở một cửa sổ PowerShell khác tại thư mục `frontend`:
```powershell
cd frontend
npm run dev
```
2. Mở trình duyệt truy cập: http://localhost:5173

---

## 3. Chạy Unit Test Backend

```powershell
cd backend
.\gradlew.bat test
```
*(Tất cả 7 test cases bao gồm MockMvc, keyword search, semantic search, validation 400 & 404 đều pass 100%).*

---

## 4. Ví dụ gọi API bằng curl

### Lấy danh sách toàn bộ khách sạn
```bash
curl http://localhost:8080/api/hotels
```

### Tìm kiếm theo từ khóa (SQL LIKE '%q%')
```bash
curl "http://localhost:8080/api/hotels/search?q=rung+thong&mode=keyword&topK=5"
```

### Tìm kiếm theo ngữ nghĩa (Vector Search qua Gemini)
```bash
curl "http://localhost:8080/api/hotels/search?q=cho+nghi+yen+tinh+giua+nui+rung+hop+cap+doi&mode=semantic&topK=5"
```

### Tìm kiếm lai (Hybrid: Semantic + Lọc theo thành phố & giá)
```bash
curl "http://localhost:8080/api/hotels/search?q=resort+gia+dinh&mode=semantic&city=Phu+Quoc&maxPrice=3000000"
```

### Tìm khách sạn tương tự
```bash
curl "http://localhost:8080/api/hotels/H001/similar?topK=5"
```

---

## 5. 4 câu truy vấn demo kiểm thử

1. `chỗ nghỉ yên tĩnh giữa núi rừng, hợp cặp đôi`
2. `nơi cho cả nhà đi chơi, có hồ bơi cho trẻ em`
3. `khách sạn sang trọng nhìn ra biển để tổ chức đám cưới`
4. `ở gần sân bay, tiết kiệm cho người đi công tác`

> Cả 4 câu truy vấn trên khi tìm ở cột **Từ khóa** sẽ trả về 0 kết quả (do mô tả khách sạn dùng từ đồng nghĩa, tránh từ khóa trực tiếp). Ngược lại, cột **Ngữ nghĩa (Gemini)** sẽ tìm ra chính xác các khách sạn phù hợp nhất.
