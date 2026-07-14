package com.rikkei.refactoring;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

// Assuming User, ShowTime, Seat, Ticket are available in the com.rikkei.refactoring package 
// (or imported/supplied by the environment)

public class TicketingService {
    private final SeatPricingService seatPricingService;
    private final DiscountRegistry discountRegistry;
    private final LoyaltyPointsCalculator loyaltyPointsCalculator;
    private final NotificationService notificationService;

    // Default constructor to preserve compatibility with existing instantiation (e.g., new TicketingService())
    public TicketingService() {
        List<SeatPricingStrategy> seatStrategies = new ArrayList<>();
        seatStrategies.add(new VipSeatPricing());
        seatStrategies.add(new SweetboxSeatPricing());
        seatStrategies.add(new NormalSeatPricing());
        this.seatPricingService = new SeatPricingService(seatStrategies, new NormalSeatPricing());

        this.discountRegistry = new DiscountRegistry(new NoDiscount());
        this.discountRegistry.register("STUDENT", new StudentDiscount());
        this.discountRegistry.register("FESTIVAL", new FestivalDiscount());

        this.loyaltyPointsCalculator = new DefaultLoyaltyPointsCalculator();
        this.notificationService = new PushNotificationService();
    }

    // Overloaded constructor for Dependency Injection
    public TicketingService(
            SeatPricingService seatPricingService,
            DiscountRegistry discountRegistry,
            LoyaltyPointsCalculator loyaltyPointsCalculator,
            NotificationService notificationService
    ) {
        this.seatPricingService = seatPricingService;
        this.discountRegistry = discountRegistry;
        this.loyaltyPointsCalculator = loyaltyPointsCalculator;
        this.notificationService = notificationService;
    }

    public Ticket bookTicket(User user, ShowTime show, List<Seat> seats, String discountCode) {
        double total = 0;
        for (Seat seat : seats) {
            total += seatPricingService.calculatePrice(show.getBasePrice(), seat.getType());
        }

        DiscountStrategy discountStrategy = discountRegistry.getStrategy(discountCode);
        total = discountStrategy.applyDiscount(total);

        int loyaltyPoints = loyaltyPointsCalculator.calculatePoints(total);
        user.addPoints(loyaltyPoints);

        notificationService.sendNotification(user, "Ticket booked!");

        return new Ticket(user, show, seats, total);
    }
}

// =========================================================================
// OCP SEAT PRICING STRATEGY
// =========================================================================

interface SeatPricingStrategy {
    boolean supports(String seatType);
    double calculatePrice(double basePrice);
}

class NormalSeatPricing implements SeatPricingStrategy {
    @Override
    public boolean supports(String seatType) {
        return "NORMAL".equalsIgnoreCase(seatType);
    }

    @Override
    public double calculatePrice(double basePrice) {
        return basePrice;
    }
}

class VipSeatPricing implements SeatPricingStrategy {
    @Override
    public boolean supports(String seatType) {
        return "VIP".equalsIgnoreCase(seatType);
    }

    @Override
    public double calculatePrice(double basePrice) {
        return basePrice + 20000;
    }
}

class SweetboxSeatPricing implements SeatPricingStrategy {
    @Override
    public boolean supports(String seatType) {
        return "SWEETBOX".equalsIgnoreCase(seatType);
    }

    @Override
    public double calculatePrice(double basePrice) {
        return basePrice + 50000;
    }
}

class SeatPricingService {
    private final List<SeatPricingStrategy> strategies;
    private final SeatPricingStrategy defaultStrategy;

    public SeatPricingService(List<SeatPricingStrategy> strategies, SeatPricingStrategy defaultStrategy) {
        this.strategies = strategies;
        this.defaultStrategy = defaultStrategy;
    }

    public double calculatePrice(double basePrice, String seatType) {
        if (seatType != null) {
            for (SeatPricingStrategy strategy : strategies) {
                if (strategy.supports(seatType)) {
                    return strategy.calculatePrice(basePrice);
                }
            }
        }
        return defaultStrategy.calculatePrice(basePrice);
    }
}

// =========================================================================
// OCP DISCOUNT STRATEGY
// =========================================================================

interface DiscountStrategy {
    double applyDiscount(double originalAmount);
}

class StudentDiscount implements DiscountStrategy {
    @Override
    public double applyDiscount(double originalAmount) {
        return originalAmount * 0.9;
    }
}

class FestivalDiscount implements DiscountStrategy {
    @Override
    public double applyDiscount(double originalAmount) {
        return originalAmount - 40000;
    }
}

class NoDiscount implements DiscountStrategy {
    @Override
    public double applyDiscount(double originalAmount) {
        return originalAmount;
    }
}

class DiscountRegistry {
    private final Map<String, DiscountStrategy> strategies = new HashMap<>();
    private final DiscountStrategy defaultStrategy;

    public DiscountRegistry(DiscountStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public void register(String code, DiscountStrategy strategy) {
        if (code != null) {
            strategies.put(code.toUpperCase(), strategy);
        }
    }

    public DiscountStrategy getStrategy(String discountCode) {
        if (discountCode == null) {
            return defaultStrategy;
        }
        return strategies.getOrDefault(discountCode.toUpperCase(), defaultStrategy);
    }
}

// =========================================================================
// OCP LOYALTY AND NOTIFICATION SERVICES
// =========================================================================

interface LoyaltyPointsCalculator {
    int calculatePoints(double totalAmount);
}

class DefaultLoyaltyPointsCalculator implements LoyaltyPointsCalculator {
    @Override
    public int calculatePoints(double totalAmount) {
        return (int) (totalAmount / 10000);
    }
}

interface NotificationService {
    void sendNotification(User user, String message);
}

class PushNotificationService implements NotificationService {
    @Override
    public void sendNotification(User user, String message) {
        System.out.println("Push notification to user device: " + message);
    }
}
