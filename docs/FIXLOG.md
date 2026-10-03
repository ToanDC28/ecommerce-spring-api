# Nhật ký fix backend (`ecommerce-spring-api`)

> Stack: Spring Boot 3.5 / Java 21 / JPA + PostgreSQL / Redis / JWT. Style: `topic-schedule-java-style` (layered `module/foo`).

## 1. Restructure: package phẳng → `module/foo`

- **Vấn đề:** code nằm phẳng `controller/ service/ repository/ entity/ request/ response/` — khó mở rộng, DTO/mapper trộn lẫn, tên class `*Implement` sai quy ước.
- **Cách sửa:** chuyển sang `module/{product,brand,type,supplier,user,role,auth,material,inventory,workorder,invoice,sales,purchasing,payment,payroll,report,customer}/` — mỗi module đủ `controller/ service/ service/impl/ repository/ entity/ dto/{request,response,mapper}/ *ApiExamples.java`; dùng chung đặt ở `module/base/*`, `exception/*`, `utils/specification/*`. Đổi `*Implement` → `*Impl`, xóa `request/base`, `response/base`, `specification/` cũ.
- **Giải thích:** 1 domain = 1 thư mục, đọc/viết/tests gói gọn; shared không duplicate.

## 2. Chuẩn response: `ApiResponse` + `PageResponse`

- **Vấn đề:** controller trả `ResponseEntity<T>` đủ kiểu, lỗi trả `Map`, service trả `Page<Entity>` trực tiếp.
- **Cách sửa:** mọi controller trả `ApiResponse.<T>builder().statusCode().message().data()` (trừ download file); service phân trang trả `PageResponse` (không lộ Spring `Page`); `GlobalExceptionHandler` trả `ApiResponse` cho 404/400/401/403/validation/500. Xóa `BaseResponse/ErrorResponse`.
- **Giải thích:** FE chỉ cần 1 envelope để unwrap + hiện message tiếng Việt thống nhất.

## 3. Exception có kiểu + soft-delete

- **Vấn đề:** `return null`, `throws Exception`, `IllegalArgumentException` chung chung; `delete()` xóa cứng; `Optional.get()` sau `isEmpty`.
- **Cách sửa:** `ResourceNotFoundException` (404) + `BusinessValidationException` (400); `BaseEntity` thêm `deletedAt` + mọi entity `@SQLDelete/@SQLRestriction`; `orElseThrow` typed; giữ `int id` theo quyết định (không migrate Long).
- **Giải thích:** lỗi có mã + message rõ cho UI; lịch sử GRN/GIN/invoice không mất khi "xóa".

## 4. Repository + Entity đúng JPA

- **Vấn đề:** chỉ extends `JpaRepository`; `ProductRepository` khai báo method trả `Specification` (sai) + native `SELECT *`; `Brand/Type.products` thiếu `@Builder.Default` (dễ NPE); thiếu index.
- **Cách sửa:** tất cả extends thêm `JpaSpecificationExecutor`; `findByName/existsByName`, bỏ native SQL; `@Getter/@Setter` thay `@Data` cho entity; `EAGER` giữ nguyên ở `User.roles/Role.permissions` (LAZY sẽ vỡ auth ngoài transaction — có chủ ý).
- **Giải thích:** Specification là đường chuẩn cho search/filter + `PageResponse`.

## 5. Service/Controller theo style

- **Vấn đề:** thiếu `@Transactional`, tên method PascalCase, thiếu `@Valid`, `@RequestParam` rời rạc, không có Swagger docs.
- **Cách sửa:** `@Transactional` (ghi) / `readOnly` (đọc); `final` + `@RequiredArgsConstructor`; `@Slf4j` + log `id=`; `Search*Request extends BaseFilterRequest` với `toSort()` whitelist + `@ParameterObject`; `@Operation/@ApiResponses` trỏ `*ApiExamples`; `@PreAuthorize("hasAnyAuthority('DOMAIN_ACTION')")`, giữ alias `*_WRITE` cũ để tương thích DB quyền.
- **Giải thích:** transaction đúng + tài liệu API sống + phân quyền mịn theo nghiệp vụ.

## 6. Mapper + DTO + Tests

- **Vấn đề:** mapping nằm rải rác trong service (dễ NPE `getBrand().getId()`), DTO thiếu validation/`@Schema`, không có test.
- **Cách sửa:** `*Mapper @Component` null-guard dòng đầu + `updateX` partial; request `@NotBlank/@NotNull + @Schema`; test mẫu `MockitoExtension + @Nested + AssertJ` (success/notFound/duplicate/validation + `verify(never())`).
- **Giải thích:** mapper không chứa logic nghiệp vụ; test là hợp đồng chống regression.

## 7. Lỗi startup `findByIdForUpdate` (user báo)

- **Vấn đề:** `No property 'forUpdate' found` — Spring Data đọc `ForUpdate` trong tên method thành property `id.forUpdate`.
- **Cách sửa:** `InvoiceRepository.java` giữ tên method nhưng gắn `@Query("select i from Invoice i where i.id = :id")` tường minh + `@Lock(PESSIMISTIC_WRITE)` (+ import `Query/Param`). Quy tắc: method tên dài kiểu `...ForUpdate/WithLock` luôn phải có `@Query`.
- **Giải thích:** lock bi quan chống 2 thu ngân thu trùng 1 invoice.

## 8. Chống mồ côi phiếu DRAFT rỗng

- **Vấn đề:** `create()` ở SO/GIN/GRN/PO/WO/invoice `save()` phiếu rỗng trước rồi mới validate vật tư → rớt validate để lại phiếu rác.
- **Cách sửa:** preload + validate toàn bộ (tồn tại/active/giá/hạn mức) **trước** `save()` đầu tiên; các bước consume/confirm nhiều dòng nằm chung 1 `@Transactional` nên lỗi là rollback hết.
- **Giải thích:** DB không bao giờ có phiếu DRAFT không dòng.

## 9. Báo `Duplicate class: InventoryServiceImpl` (user báo)

- **Kết quả kiểm tra:** source chỉ có đúng 1 định nghĩa (`module/inventory/service/impl/`), không có file trùng, `target/` không còn class package cũ → **không phải lỗi code**.
- **Cách sửa:** `mvn clean` (xóa `target/`) → IntelliJ *File → Invalidate Caches / Restart* → Reimport Maven → rebuild. Nguyên nhân: đợt di chuyển hàng loạt file ngoài IDE làm index/build cũ còn sót.
