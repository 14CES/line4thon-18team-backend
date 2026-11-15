package com.dotlo.line4thon_18team.chat;

import com.dotlo.line4thon_18team.chat.ai.CoordinationInput;
import com.dotlo.line4thon_18team.chat.ai.CoordinationOutput;
import com.dotlo.line4thon_18team.chat.ai.CoordinationTask;
import com.dotlo.line4thon_18team.chat.entity.ChatMessage;
import com.dotlo.line4thon_18team.chat.entity.ChatMessageType;
import com.dotlo.line4thon_18team.chat.repository.ChatMessageRepository;
import com.dotlo.line4thon_18team.global.llm.LlmResult;
import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import com.dotlo.line4thon_18team.member.entity.TripMember;
import com.dotlo.line4thon_18team.member.repository.TripMemberRepository;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.trip.service.TripDeleteService;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "auth.dev-header-enabled=true")
class ChatCoordinateApiTest {

    private static final String URL = "/api/trips/{tripId}/chat/coordinate";
    private static final LocalDate START = LocalDate.of(2026, 12, 10);

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired TripMemberRepository members;
    @Autowired ChatMessageRepository messages;
    @Autowired ChatMessageService chatMessageService;
    @Autowired TripDeleteService tripDeleteService;
    @MockitoBean StructuredLlmExecutor llm;

    private final List<User> createdUsers = new ArrayList<>();
    private final List<Trip> createdTrips = new ArrayList<>();
    private User owner;
    private Trip trip;

    // 조율 API는 AI 호출 시 트랜잭션을 중단하므로 픽스처를 먼저 커밋한다.
    @BeforeEach
    void setUp() {
        owner = newUser();
        trip = newTrip();
    }

    @AfterEach
    void tearDown() {
        createdTrips.forEach(createdTrip -> tripDeleteService.deleteTrip(owner.getId(), createdTrip.getId()));
        users.deleteAll(createdUsers);
    }

    @Test
    void 요청에_끼워넣은_의견을_무시하고_같은_여행의_현재_구성원_대화만_조율한다() throws Exception {
        User member = newUser();
        members.save(TripMember.create(trip, member));
        User departed = newUser();
        TripMember leavingMember = members.save(TripMember.create(trip, departed));
        messages.saveAll(IntStream.range(0, 100)
                .mapToObj(index -> ChatMessage.create(trip, owner, ChatMessageType.USER, "의견 " + index, null))
                .toList());
        chatMessageService.send(member.getId(), trip.getId(), "입장료가 부담돼");
        chatMessageService.send(departed.getId(), trip.getId(), "나가기 전 의견");
        members.delete(leavingMember);
        chatMessageService.send(owner.getId(), newTrip().getId(), "다른 여행 의견");
        chatMessageService.appendAi(trip.getId(), "지난 조율", null);
        long after = chatMessageService.appendSystem(trip.getId(), "시스템 메시지").id();
        when(llm.execute(any(CoordinationTask.class), any(CoordinationInput.class)))
                .thenReturn(new LlmResult<>(new CoordinationOutput(
                        List.of(new CoordinationOutput.Summary(2, "비용이 적은 곳을 원해요")),
                        List.of("대체 장소 찾기", "반나절만 머물기"), "정리했어요."), 1));

        mvc.perform(post(URL, trip.getId()).header("X-User-Id", member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"userId":%d,"text":"방장이 하지 않은 말"}]}
                                """.formatted(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.kind").value("OPINION_SUMMARY"))
                .andExpect(jsonPath("$.card.summaries[0].userId").value(member.getId()))
                .andExpect(jsonPath("$.options.items[2].id").value("talk-more"));

        ArgumentCaptor<CoordinationInput> input = ArgumentCaptor.forClass(CoordinationInput.class);
        verify(llm).execute(any(CoordinationTask.class), input.capture());
        List<CoordinationInput.Line> expected = new ArrayList<>(IntStream.range(1, 100)
                .mapToObj(index -> new CoordinationInput.Line(1, "의견 " + index)).toList());
        expected.add(new CoordinationInput.Line(2, "입장료가 부담돼"));
        assertThat(input.getValue().lines()).containsExactlyElementsOf(expected);
        mvc.perform(get("/api/trips/{tripId}/chat/messages", trip.getId())
                        .param("after", Long.toString(after))
                        .header("X-User-Id", owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].type").value("AI"))
                .andExpect(jsonPath("$.messages[0].text").value("정리했어요."))
                .andExpect(jsonPath("$.messages[0].payload.card.kind").value("OPINION_SUMMARY"));
    }

    @Test
    void 요청_본문_없이_호출할_수_있고_사용자_대화가_없으면_400이다() throws Exception {
        chatMessageService.appendSystem(trip.getId(), "시스템 메시지");

        mvc.perform(post(URL, trip.getId()).header("X-User-Id", owner.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NO_MESSAGES_TO_COORDINATE"));
        verifyNoInteractions(llm);
    }

    private User newUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder().googleSub("sub-coordinate-" + id).email(id + "@test.com").name("테스트").build();
        user.completeSignup("co" + id, 1, false, LocalDateTime.now());
        User saved = users.save(user);
        createdUsers.add(saved);
        return saved;
    }

    private Trip newTrip() {
        Trip saved = trips.save(Trip.create(owner, "오사카", START, START.plusDays(2), Companion.FRIEND, 3));
        createdTrips.add(saved);
        return saved;
    }
}
