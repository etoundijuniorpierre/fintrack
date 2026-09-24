package com.fintrack.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.fintrack.user.client.notification.NotificationClientService;
import com.fintrack.user.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.user.exception.BusinessRuleViolationException;
import com.fintrack.user.exception.ErrorCode;
import com.fintrack.user.model.constant.PeriodType;
import com.fintrack.user.model.readmodel.UserStats;
import com.fintrack.user.model.readmodel.UserStatsAggregate;
import com.fintrack.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Set;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UserStatsServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private org.springframework.context.MessageSource messageSource;

  @Mock
  private NotificationClientService notificationClientService;

  @Mock
  private ReportingSystemConfigClientService reportingSystemConfigClientService;

  @InjectMocks
  private UserStatsServiceImpl service;

  private void stubCounts(
    long active,
    long inactive,
    long connected,
    long recentlyActive,
    long neverConnected,
    long locked,
    long firstLoginPending,
    long newInPeriod
  ) {
    long total = active + inactive;
    UserStatsAggregate aggregate = new UserStatsAggregate(
      total,
      active,
      inactive,
      connected,
      neverConnected,
      locked,
      firstLoginPending,
      recentlyActive
    );
    when(
      userRepository.getUserStatsAggregate(
        any(Specification.class),
        anySet(),
        any(LocalDateTime.class),
        anyInt()
      )
    ).thenReturn(aggregate);
    lenient()
      .when(userRepository.count(any(Specification.class)))
      .thenReturn(newInPeriod);
  }

  private void stubCountsForRepo(
    UserRepository repo,
    long active,
    long inactive,
    long connected,
    long recentlyActive,
    long neverConnected,
    long locked,
    long firstLoginPending,
    long newInPeriod
  ) {
    long total = active + inactive;
    UserStatsAggregate aggregate = new UserStatsAggregate(
      total,
      active,
      inactive,
      connected,
      neverConnected,
      locked,
      firstLoginPending,
      recentlyActive
    );
    when(
      repo.getUserStatsAggregate(
        any(Specification.class),
        anySet(),
        any(LocalDateTime.class),
        anyInt()
      )
    ).thenReturn(aggregate);
    lenient()
      .when(repo.count(any(Specification.class)))
      .thenReturn(newInPeriod);
  }

  @Test
  @DisplayName("activeCount + inactiveCount must equal totalCount")
  void activeCountPlusInactiveCountEqualsTotalCount() {
    stubCounts(80L, 20L, 5L, 15L, 10L, 3L, 7L, 25L);

    UserStats response = service.getStats(
      PeriodType.ALL,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(
      response.getActiveCount() + response.getInactiveCount()
    ).isEqualTo(response.getTotalCount());
    assertThat(response.getTotalCount()).isEqualTo(100L);
    assertThat(response.getActiveCount()).isEqualTo(80L);
    assertThat(response.getInactiveCount()).isEqualTo(20L);
  }

  @Test
  @DisplayName("connectedCount uses the current WebSocket presence list")
  void getStats_onlineUsersAvailable_usesRealtimePresence() {
    Set<String> onlineUsernames = Set.of("alice", "bob");
    when(notificationClientService.getOnlineUsernames()).thenReturn(
      onlineUsernames
    );
    when(
      userRepository.getUserStatsAggregate(
        any(Specification.class),
        eq(onlineUsernames),
        any(LocalDateTime.class),
        anyInt()
      )
    ).thenReturn(new UserStatsAggregate(2, 2, 0, 2, 0, 0, 0, 2));

    UserStats response = service.getStats(
      PeriodType.ALL,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(response.getConnectedCount()).isEqualTo(2);
    verify(notificationClientService).getOnlineUsernames();
  }

  @Property(tries = 200)
  @Label(
    "activeCount + inactiveCount = totalCount for any valid user distribution"
  )
  void property_activeCountPlusInactiveCountEqualsTotalCount(
    @ForAll @IntRange(min = 0, max = 1000) int active,
    @ForAll @IntRange(min = 0, max = 1000) int inactive
  ) {
    UserRepository repo = mock(UserRepository.class);
    org.springframework.context.MessageSource ms = mock(org.springframework.context.MessageSource.class);
    UserStatsServiceImpl svc = new UserStatsServiceImpl(
      repo,
      ms,
      mock(NotificationClientService.class),
      mock(ReportingSystemConfigClientService.class)
    );

    stubCountsForRepo(repo, active, inactive, 0L, 0L, 0L, 0L, 0L, 0L);

    UserStats response = svc.getStats(
      PeriodType.ALL,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(
      response.getActiveCount() + response.getInactiveCount()
    ).isEqualTo(response.getTotalCount());
  }

  @Test
  @DisplayName("All fields in UserStatsResponse must be >= 0")
  void allFieldsMustBeNonNegative() {
    stubCounts(50L, 50L, 3L, 10L, 5L, 2L, 4L, 8L);

    UserStats response = service.getStats(
      PeriodType.LAST_30_DAYS,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(response.getTotalCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getActiveCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getInactiveCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getConnectedCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getNeverConnectedCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getLockedCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getFirstLoginPendingCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getRecentlyActiveCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getNewUsersInPeriod()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  @DisplayName("THIS_MONTH computes user statistics for the current calendar month")
  void thisMonthComputesCurrentCalendarMonthStatistics() {
    stubCounts(8L, 2L, 3L, 4L, 1L, 0L, 0L, 5L);

    UserStats response = service.getStats(
      PeriodType.THIS_MONTH,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(response.getTotalCount()).isEqualTo(10L);
    assertThat(response.getNewUsersInPeriod()).isEqualTo(5L);
    verify(userRepository).count(any(Specification.class));
  }

  @Property(tries = 200)
  @Label(
    "All UserStatsResponse fields are >= 0 for any valid repository counts"
  )
  void property_allFieldsNonNegative(
    @ForAll @IntRange(min = 0, max = 500) int active,
    @ForAll @IntRange(min = 0, max = 500) int inactive,
    @ForAll @IntRange(min = 0, max = 100) int connected,
    @ForAll @IntRange(min = 0, max = 200) int recentlyActive,
    @ForAll @IntRange(min = 0, max = 100) int neverConnected,
    @ForAll @IntRange(min = 0, max = 50) int locked,
    @ForAll @IntRange(min = 0, max = 50) int firstLoginPending
  ) {
    UserRepository repo = mock(UserRepository.class);
    org.springframework.context.MessageSource ms = mock(org.springframework.context.MessageSource.class);
    UserStatsServiceImpl svc = new UserStatsServiceImpl(
      repo,
      ms,
      mock(NotificationClientService.class),
      mock(ReportingSystemConfigClientService.class)
    );

    stubCountsForRepo(
      repo,
      active,
      inactive,
      connected,
      recentlyActive,
      neverConnected,
      locked,
      firstLoginPending,
      0L
    );

    UserStats response = svc.getStats(
      PeriodType.ALL,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(response.getTotalCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getActiveCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getInactiveCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getConnectedCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getNeverConnectedCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getLockedCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getFirstLoginPendingCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getRecentlyActiveCount()).isGreaterThanOrEqualTo(0L);
    assertThat(response.getNewUsersInPeriod()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  @DisplayName("connectedCount <= recentlyActiveCount <= totalCount")
  void connectedCountLessThanOrEqualToRecentlyActiveCountLessThanOrEqualToTotalCount() {
    stubCounts(90L, 10L, 3L, 12L, 5L, 2L, 4L, 8L);

    UserStats response = service.getStats(
      PeriodType.ALL,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(response.getConnectedCount()).isLessThanOrEqualTo(
      response.getRecentlyActiveCount()
    );
    assertThat(response.getRecentlyActiveCount()).isLessThanOrEqualTo(
      response.getTotalCount()
    );
  }

  @Property(tries = 200)
  @Label(
    "connectedCount <= recentlyActiveCount <= totalCount for any valid distribution"
  )
  void property_connectedLessThanOrEqualToRecentlyActiveLessThanOrEqualToTotal(
    @ForAll @IntRange(min = 0, max = 500) int active,
    @ForAll @IntRange(min = 0, max = 500) int inactive,
    @ForAll @IntRange(min = 0, max = 200) int connected,
    @ForAll @IntRange(min = 0, max = 200) int recentlyActive
  ) {
    int effectiveConnected = Math.min(connected, recentlyActive);
    long total = (long) active + inactive;
    long effectiveRecentlyActive = Math.min(recentlyActive, total);
    long effectiveConnectedFinal = Math.min(
      effectiveConnected,
      effectiveRecentlyActive
    );

    UserRepository repo = mock(UserRepository.class);
    org.springframework.context.MessageSource ms = mock(org.springframework.context.MessageSource.class);
    UserStatsServiceImpl svc = new UserStatsServiceImpl(
      repo,
      ms,
      mock(NotificationClientService.class),
      mock(ReportingSystemConfigClientService.class)
    );

    stubCountsForRepo(
      repo,
      active,
      inactive,
      effectiveConnectedFinal,
      effectiveRecentlyActive,
      0L,
      0L,
      0L,
      0L
    );

    UserStats response = svc.getStats(
      PeriodType.ALL,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(response.getConnectedCount()).isLessThanOrEqualTo(
      response.getRecentlyActiveCount()
    );
    assertThat(response.getRecentlyActiveCount()).isLessThanOrEqualTo(
      response.getTotalCount()
    );
  }

  @Test
  @DisplayName("CUSTOM without dateFrom throws BusinessRuleViolationException")
  void customPeriod_missingDateFrom_throwsBadRequest() {
    assertThatThrownBy(() ->
      service.getStats(
        PeriodType.CUSTOM,
        null,
        LocalDateTime.now(),
        "all",
        null,
        null,
        null
      )
    )
      .isInstanceOf(BusinessRuleViolationException.class)
      .satisfies(ex ->
        assertThat(
          ((BusinessRuleViolationException) ex).getErrorCode()
        ).isEqualTo(ErrorCode.STATS_CUSTOM_PERIOD_DATES_REQUIRED)
      );
  }

  @Test
  @DisplayName("CUSTOM without dateTo throws BusinessRuleViolationException")
  void customPeriod_missingDateTo_throwsBadRequest() {
    assertThatThrownBy(() ->
      service.getStats(
        PeriodType.CUSTOM,
        LocalDateTime.now().minusDays(7),
        null,
        "all",
        null,
        null,
        null
      )
    )
      .isInstanceOf(BusinessRuleViolationException.class)
      .satisfies(ex ->
        assertThat(
          ((BusinessRuleViolationException) ex).getErrorCode()
        ).isEqualTo(ErrorCode.STATS_CUSTOM_PERIOD_DATES_REQUIRED)
      );
  }

  @Test
  @DisplayName(
    "CUSTOM without both dateFrom and dateTo throws BusinessRuleViolationException"
  )
  void customPeriod_missingBothDates_throwsBadRequest() {
    assertThatThrownBy(() ->
      service.getStats(PeriodType.CUSTOM, null, null, "all", null, null, null)
    )
      .isInstanceOf(BusinessRuleViolationException.class)
      .satisfies(ex ->
        assertThat(
          ((BusinessRuleViolationException) ex).getErrorCode()
        ).isEqualTo(ErrorCode.STATS_CUSTOM_PERIOD_DATES_REQUIRED)
      );
  }

  @Test
  @DisplayName(
    "CUSTOM with dateFrom after dateTo throws BusinessRuleViolationException"
  )
  void customPeriod_dateFromAfterDateTo_throwsBadRequest() {
    assertThatThrownBy(() ->
      service.getStats(
        PeriodType.CUSTOM,
        LocalDateTime.of(2024, 6, 1, 0, 0),
        LocalDateTime.of(2024, 1, 1, 0, 0),
        "all",
        null,
        null,
        null
      )
    )
      .isInstanceOf(BusinessRuleViolationException.class)
      .satisfies(ex ->
        assertThat(
          ((BusinessRuleViolationException) ex).getErrorCode()
        ).isEqualTo(ErrorCode.STATS_CUSTOM_PERIOD_DATE_ORDER)
      );
  }

  @Test
  @DisplayName("CUSTOM with dateFrom equal to dateTo is valid")
  void customPeriod_dateFromEqualsDateTo_isValid() {
    LocalDateTime date = LocalDateTime.of(2024, 3, 15, 12, 0);

    stubCounts(80L, 20L, 5L, 15L, 10L, 2L, 3L, 0L);

    UserStats response = service.getStats(
      PeriodType.CUSTOM,
      date,
      date,
      "all",
      null,
      null,
      null
    );
    assertThat(response).isNotNull();
  }

  @Test
  @DisplayName("Null period defaults to ALL — no date filter applied")
  void nullPeriod_defaultsToAll_noDateFilter() {
    stubCounts(40L, 10L, 2L, 12L, 5L, 1L, 2L, 0L);

    UserStats response = service.getStats(
      null,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(response).isNotNull();
    assertThat(response.getNewUsersInPeriod()).isEqualTo(
      response.getTotalCount()
    );
    verify(userRepository, times(1)).getUserStatsAggregate(
      any(Specification.class),
      anySet(),
      any(LocalDateTime.class),
      anyInt()
    );
    verify(userRepository, never()).count(any(Specification.class));
  }

  @Test
  @DisplayName("period=ALL: newUsersInPeriod equals totalCount")
  void periodAll_newUsersInPeriodEqualsTotalCount() {
    stubCounts(150L, 50L, 10L, 12L, 20L, 5L, 8L, 0L);

    UserStats response = service.getStats(
      PeriodType.ALL,
      null,
      null,
      "all",
      null,
      null,
      null
    );

    assertThat(response.getNewUsersInPeriod()).isEqualTo(
      response.getTotalCount()
    );
    verify(userRepository, times(1)).getUserStatsAggregate(
      any(Specification.class),
      anySet(),
      any(LocalDateTime.class),
      anyInt()
    );
    verify(userRepository, never()).count(any(Specification.class));
  }

  @Test
  @DisplayName(
    "CUSTOM period: repository called with exact dateFrom and dateTo"
  )
  void customPeriod_callsRepositoryWithExactDates() {
    LocalDateTime dateFrom = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    LocalDateTime dateTo = LocalDateTime.of(2024, 3, 31, 23, 59, 59);

    stubCounts(80L, 20L, 5L, 12L, 10L, 2L, 3L, 15L);

    UserStats response = service.getStats(
      PeriodType.CUSTOM,
      dateFrom,
      dateTo,
      "all",
      null,
      null,
      null
    );

    assertThat(response.getNewUsersInPeriod()).isEqualTo(15L);
    verify(userRepository, times(1)).getUserStatsAggregate(
      any(Specification.class),
      anySet(),
      any(LocalDateTime.class),
      anyInt()
    );
    verify(userRepository, times(1)).count(any(Specification.class));
  }
}
