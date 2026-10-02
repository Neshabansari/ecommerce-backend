package com.ecommerce.repository;

import com.ecommerce.dto.TopProductResponse;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // A user's order history, newest first. Items are loaded in the same query.
    @EntityGraph(attributePaths = "items")
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Finds an order only if it belongs to the given user.
    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // Admin list. Only the customer is joined here: joining the items as well would force
    // Hibernate to paginate in memory. Items load afterwards in batches instead.
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    // The entity is written with its full name because ORDER is also an HQL keyword.
    @Query("select sum(o.totalAmount) from com.ecommerce.model.Order o where o.status in :statuses")
    BigDecimal sumTotalByStatusIn(@Param("statuses") Collection<OrderStatus> statuses);

    @Query("""
            select new com.ecommerce.dto.TopProductResponse(
                i.product.id, i.product.name, sum(i.quantity), sum(i.priceAtPurchase * i.quantity))
            from com.ecommerce.model.Order o join o.items i
            where o.status in :statuses
            group by i.product.id, i.product.name
            order by sum(i.quantity) desc, i.product.name asc
            """)
    List<TopProductResponse> findTopSelling(@Param("statuses") Collection<OrderStatus> statuses,
                                            Pageable pageable);
}