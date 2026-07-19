package com.retail.promotion.service;

import com.retail.dto.PromotionDetailRequest;
import com.retail.dto.PromotionRequest;
import com.retail.dto.PromotionResponse;
import com.retail.entity.*;
import com.retail.exception.ValidationException;
import com.retail.repository.EmployeeRepository;
import com.retail.repository.ProductRepository;
import com.retail.repository.PromotionDetailRepository;
import com.retail.repository.PromotionRepository;
import com.retail.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionDetailRepository promotionDetailRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private com.retail.repository.AuditLogRepository auditLogRepository;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    private Employee employee;
    private Product product;
    private Promotion promotion;

    @BeforeEach
    void setUp() {
        employee = Employee.builder()
                .employeeId(1L)
                .username("admin")
                .fullName("Admin User")
                .build();

        product = Product.builder()
                .productId(10L)
                .sku("SKU10")
                .productName("Sản phẩm 10")
                .build();

        promotion = Promotion.builder()
                .promotionId(100L)
                .promotionName("Chương trình KM")
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(2))
                .status(PromotionStatus.Draft)
                .createdBy(employee)
                .build();
    }

    // ─── UNIT TEST FOR SERVICE BUSINESS LOGIC & VALIDATION ───────────────────

    @Test
    void create_ShouldSaveWithStatusDraft_WhenRequestIsValid() {
        // Arrange
        PromotionRequest request = PromotionRequest.builder()
                .promotionName("Khuyến mãi hè")
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(5))
                .details(List.of(
                        PromotionDetailRequest.builder()
                                .productId(10L)
                                .discountType("Percentage")
                                .discountValue(BigDecimal.valueOf(15))
                                .build()
                ))
                .build();

        given(promotionRepository.existsByPromotionName("Khuyến mãi hè")).willReturn(false);
        given(employeeRepository.findByUsername("admin")).willReturn(Optional.of(employee));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(promotionRepository.save(any(Promotion.class))).willAnswer(invocation -> {
            Promotion p = invocation.getArgument(0);
            p.setPromotionId(1L);
            return p;
        });

        // Act
        PromotionResponse response = promotionService.create(request, "admin");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PromotionStatus.Draft);
        assertThat(response.getPromotionName()).isEqualTo("Khuyến mãi hè");
        assertThat(response.getDetails()).hasSize(1);
        assertThat(response.getDetails().get(0).getProductId()).isEqualTo(10L);
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void create_ShouldThrowValidationException_WhenPercentageDiscountIsOutOfRange() {
        // Arrange (Percentage > 100)
        PromotionRequest request = PromotionRequest.builder()
                .promotionName("Khuyến mãi sai")
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(5))
                .details(List.of(
                        PromotionDetailRequest.builder()
                                .productId(10L)
                                .discountType("Percentage")
                                .discountValue(BigDecimal.valueOf(101))
                                .build()
                ))
                .build();

        given(promotionRepository.existsByPromotionName("Khuyến mãi sai")).willReturn(false);
        given(employeeRepository.findByUsername("admin")).willReturn(Optional.of(employee));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> promotionService.create(request, "admin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Giảm giá theo phần trăm phải trong khoảng (0, 100]");
    }

    @Test
    void create_ShouldThrowValidationException_WhenFixedAmountDiscountIsLessThanOrEqualToZero() {
        // Arrange (FixedAmount <= 0)
        PromotionRequest request = PromotionRequest.builder()
                .promotionName("Khuyến mãi sai")
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(5))
                .details(List.of(
                        PromotionDetailRequest.builder()
                                .productId(10L)
                                .discountType("FixedAmount")
                                .discountValue(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        given(promotionRepository.existsByPromotionName("Khuyến mãi sai")).willReturn(false);
        given(employeeRepository.findByUsername("admin")).willReturn(Optional.of(employee));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> promotionService.create(request, "admin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Giảm giá theo tiền mặt phải lớn hơn 0");
    }

    @Test
    void create_ShouldThrowValidationException_WhenDuplicateProductsInRequest() {
        // Arrange (duplicate product IDs)
        PromotionRequest request = PromotionRequest.builder()
                .promotionName("Khuyến mãi trùng")
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(5))
                .details(List.of(
                        PromotionDetailRequest.builder()
                                .productId(10L)
                                .discountType("Percentage")
                                .discountValue(BigDecimal.valueOf(10))
                                .build(),
                        PromotionDetailRequest.builder()
                                .productId(10L)
                                .discountType("Percentage")
                                .discountValue(BigDecimal.valueOf(20))
                                .build()
                ))
                .build();

        given(promotionRepository.existsByPromotionName("Khuyến mãi trùng")).willReturn(false);
        given(employeeRepository.findByUsername("admin")).willReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> promotionService.create(request, "admin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("bị trùng lặp trong cùng một chương trình khuyến mãi");
    }

    @Test
    void update_ShouldThrowValidationException_WhenStatusIsNotDraft() {
        // Arrange
        promotion.setStatus(PromotionStatus.Active);
        PromotionRequest request = PromotionRequest.builder()
                .promotionName("Sửa khuyến mãi")
                .build();

        given(promotionRepository.findById(100L)).willReturn(Optional.of(promotion));

        // Act & Assert
        assertThatThrownBy(() -> promotionService.update(100L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Chỉ được phép chỉnh sửa chương trình ở trạng thái Bản nháp (Draft)");
    }

    @Test
    void activate_ShouldChangeStatusToActive_WhenCurrentStatusIsDraft() {
        // Arrange
        given(promotionRepository.findById(100L)).willReturn(Optional.of(promotion));

        // Act
        promotionService.activate(100L);

        // Assert
        assertThat(promotion.getStatus()).isEqualTo(PromotionStatus.Active);
        verify(promotionRepository).save(promotion);
    }

    @Test
    void cancel_ShouldSetStatusToCanceledAndNotDeletePhysically() {
        // Arrange
        given(promotionRepository.findById(100L)).willReturn(Optional.of(promotion));

        // Act
        promotionService.cancel(100L);

        // Assert
        assertThat(promotion.getStatus()).isEqualTo(PromotionStatus.Canceled);
        verify(promotionRepository).save(promotion);
        verify(promotionRepository, never()).delete(any(Promotion.class));
        verify(promotionRepository, never()).deleteById(anyLong());
    }
}
