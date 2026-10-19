import { useSelector } from 'react-redux'
import type { RootState } from './app/store'
import FilterPanel from './components/FilterPanel'
import ResultColumn from './components/ResultColumn'
import SearchBar from './components/SearchBar'
import { useSearchHotelsQuery } from './features/search/hotelApi'
import { Hotel, Sparkles, ShieldCheck, Compass, Layers } from 'lucide-react'

export default function App() {
  const { query, city, maxPrice, topK } = useSelector((s: RootState) => s.search)

  const hasQuery = query.trim().length > 0

  // Gọi đồng thời cả hai mode song song
  const keywordResult = useSearchHotelsQuery(
    { q: query, mode: 'keyword', topK, city: city || undefined, maxPrice },
    { skip: !hasQuery }
  )

  const semanticResult = useSearchHotelsQuery(
    { q: query, mode: 'semantic', topK, city: city || undefined, maxPrice },
    { skip: !hasQuery }
  )

  return (
    <div className="min-h-screen bg-[#f5f7fa] flex flex-col">
      {/* Top Navigation Bar phong cách Booking.com */}
      <header className="bg-[#003b95] text-white">
        <div className="max-w-6xl mx-auto px-4 py-3.5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-2xl font-black tracking-tight text-white flex items-center gap-1.5 cursor-pointer">
              <span>MEGO</span>
              <span className="text-[#febb02]">.com</span>
            </span>
            <span className="hidden sm:inline-block text-xs bg-white/15 px-2 py-0.5 rounded text-blue-100 font-medium">
              Vector Search Prototype
            </span>
          </div>

          <div className="flex items-center gap-4 text-xs">
            <div className="hidden md:flex items-center gap-1 text-blue-200">
              <ShieldCheck className="w-4 h-4 text-yellow-400" />
              <span>Google Gemini &bull; gemini-embedding-001</span>
            </div>
            <button className="px-3 py-1.5 border border-white/40 hover:border-white rounded text-xs font-semibold transition-colors">
              Đăng ký đối tác
            </button>
            <button className="px-3 py-1.5 bg-white text-[#003b95] hover:bg-blue-50 rounded text-xs font-bold transition-colors">
              Đăng nhập
            </button>
          </div>
        </div>

        {/* Hero Section */}
        <div className="max-w-6xl mx-auto px-4 pt-6 pb-12 sm:pt-10 sm:pb-16">
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-white mb-2 leading-tight">
            Tìm chỗ nghỉ hoàn hảo bằng ngôn ngữ tự nhiên
          </h1>
          <p className="text-sm sm:text-base text-blue-100 max-w-2xl leading-relaxed">
            Trải nghiệm sức mạnh của <strong>Vector Database</strong> trong việc thấu hiểu câu hỏi đời thường, vượt xa hạn chế của tìm kiếm từ khóa SQL truyền thống.
          </p>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-6xl mx-auto px-4 flex-1 w-full -mt-7 sm:-mt-8">
        {/* Search Bar - Viền vàng Booking.com */}
        <div className="mb-4">
          <SearchBar />
        </div>

        {/* Filter Panel */}
        <div className="mb-6">
          <FilterPanel />
        </div>

        {/* Màn hình khởi đầu khi chưa tìm kiếm */}
        {!hasQuery && (
          <div className="bg-white rounded-2xl border border-gray-200 p-8 sm:p-14 text-center shadow-xs my-6">
            <div className="w-16 h-16 bg-blue-50 text-[#006ce4] rounded-2xl flex items-center justify-center mx-auto mb-4">
              <Compass className="w-8 h-8 animate-pulse" />
            </div>
            <h3 className="text-lg font-bold text-gray-800 mb-1">
              Bắt đầu trải nghiệm tìm kiếm thông minh
            </h3>
            <p className="text-xs sm:text-sm text-gray-500 max-w-lg mx-auto mb-6 leading-relaxed">
              Hãy nhập câu mô tả nhu cầu của bạn vào ô tìm kiếm hoặc chọn nhanh một trong các gợi ý mẫu ở trên để xem hai cột kết quả so sánh đối đầu.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 max-w-3xl mx-auto text-left">
              <div className="p-4 rounded-xl border border-gray-100 bg-gray-50/70">
                <div className="font-bold text-xs text-[#003b95] mb-1 flex items-center gap-1.5">
                  <Hotel className="w-4 h-4 text-blue-600" />
                  25 Khách sạn mẫu
                </div>
                <p className="text-[11px] text-gray-500 leading-normal">
                  Dữ liệu phân bố đều tại 6 thành phố: Đà Lạt, Vũng Tàu, Phú Quốc, Nha Trang, Hà Nội, TP.HCM.
                </p>
              </div>

              <div className="p-4 rounded-xl border border-gray-100 bg-gray-50/70">
                <div className="font-bold text-xs text-[#003b95] mb-1 flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-amber-500" />
                  Ngữ nghĩa tự nhiên
                </div>
                <p className="text-[11px] text-gray-500 leading-normal">
                  Mô tả không trùng lặp từ khóa trực tiếp để chứng minh độ vượt trội của AI Embedding.
                </p>
              </div>

              <div className="p-4 rounded-xl border border-gray-100 bg-gray-50/70">
                <div className="font-bold text-xs text-[#003b95] mb-1 flex items-center gap-1.5">
                  <Layers className="w-4 h-4 text-green-600" />
                  Truy vấn lai (Hybrid)
                </div>
                <p className="text-[11px] text-gray-500 leading-normal">
                  Kết hợp độ tương đồng vector cùng bộ lọc metadata thành phố và khoảng giá.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Hai Cột Kết Quả So Sánh Song Song */}
        {hasQuery && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 pb-12">
            {/* Cột 1: Tìm theo từ khóa (SQL LIKE) */}
            <ResultColumn
              title="Tìm theo Từ khóa"
              subtitle="Thuật toán SQL LIKE '%q%' — Khớp nguyên văn"
              mode="keyword"
              data={keywordResult.data}
              isLoading={keywordResult.isFetching}
              isError={keywordResult.isError}
              showScore={false}
            />

            {/* Cột 2: Tìm theo ngữ nghĩa (Vector Database) */}
            <ResultColumn
              title="Tìm theo Ngữ nghĩa"
              subtitle="Gemini Vector Search — Hiểu ý nghĩa ngữ cảnh"
              mode="semantic"
              data={semanticResult.data}
              isLoading={semanticResult.isFetching}
              isError={semanticResult.isError}
              showScore={true}
            />
          </div>
        )}
      </main>

      {/* Footer phong cách Booking.com */}
      <footer className="bg-[#00224f] text-white/80 py-8 text-xs border-t border-blue-900 mt-auto">
        <div className="max-w-6xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4 text-center sm:text-left">
          <div>
            <div className="font-bold text-sm text-white mb-1">MEGO Hotel Search Platform</div>
            <p className="text-gray-400 text-[11px]">
              Prototype nghiên cứu giải pháp Vector Database & RAG &bull; Spring AI + Gemini + React
            </p>
          </div>
          <div className="text-[11px] text-gray-400">
            &copy; 2026 MEGO Group. Bản quyền thuộc về đồ án tốt nghiệp / báo cáo công nghệ.
          </div>
        </div>
      </footer>
    </div>
  )
}
