package com.team2.postservice.resume;

import com.team2.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.resume.dto.ResumeRequestDto;
import com.team2.postservice.resume.dto.ResumeResponseDto;
import com.team2.postservice.resume.entity.Resume;
import com.team2.postservice.resume.entity.ResumeCareer;
import com.team2.postservice.resume.repository.ResumeRepository;
import com.team2.postservice.resume.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumeServiceTest {

    final ResumeRepository resumes = mock(ResumeRepository.class);
    final ResumeService service = new ResumeService(resumes);

    private ResumeRequestDto.Upsert request() {
        return new ResumeRequestDto.Upsert(
                "10년차 가전 수리 전문가",
                "세탁기·냉장고 위주로 출장 수리해드립니다.",
                List.of("세탁기", "냉장고", "에어컨"),
                List.of(
                        new ResumeCareer.CareerInput("2023 - 현재", "개인 출장 수리 서비스 운영"),
                        new ResumeCareer.CareerInput("2015 - 2023", "OO전자서비스 가전 수리 담당")
                )
        );
    }

    @Test void createsResumeWithSkillsAndCareers() {
        when(resumes.existsByUserId(200L)).thenReturn(false);
        when(resumes.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createResume(request(), 200L);

        ArgumentCaptor<Resume> captor = ArgumentCaptor.forClass(Resume.class);
        verify(resumes).save(captor.capture());
        Resume saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(200L);
        assertThat(saved.getHeadline()).isEqualTo("10년차 가전 수리 전문가");
        assertThat(saved.getSkills()).containsExactly("세탁기", "냉장고", "에어컨");
        assertThat(saved.getCareers()).hasSize(2);
        assertThat(saved.getCareers().get(0).getPeriod()).isEqualTo("2023 - 현재");
        assertThat(saved.getCareers().get(0).getSortOrder()).isEqualTo(0);
        assertThat(saved.getCareers().get(1).getSortOrder()).isEqualTo(1);
    }

    @Test void rejectsDuplicateResumeCreation() {
        when(resumes.existsByUserId(200L)).thenReturn(true);

        assertThatThrownBy(() -> service.createResume(request(), 200L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_RESUME);

        verify(resumes, never()).save(any());
    }

    @Test void rejectsBlankHeadline() {
        ResumeRequestDto.Upsert invalid = new ResumeRequestDto.Upsert("", "소개", List.of(), List.of());
        when(resumes.existsByUserId(200L)).thenReturn(false);

        assertThatThrownBy(() -> service.createResume(invalid, 200L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test void rejectsTooManySkills() {
        ResumeRequestDto.Upsert invalid = new ResumeRequestDto.Upsert(
                "제목", "소개",
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"),
                List.of()
        );
        when(resumes.existsByUserId(200L)).thenReturn(false);

        assertThatThrownBy(() -> service.createResume(invalid, 200L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test void updateReplacesSkillsAndCareersEntirely() {
        Resume existing = Resume.builder().userId(200L).build();
        existing.updateContent("옛 제목", "옛 소개", List.of("드럼세탁기"),
                List.of(new ResumeCareer.CareerInput("2020", "예전 경력")));
        when(resumes.findByUserId(200L)).thenReturn(Optional.of(existing));

        service.updateResume(request(), 200L);

        assertThat(existing.getHeadline()).isEqualTo("10년차 가전 수리 전문가");
        assertThat(existing.getSkills()).containsExactly("세탁기", "냉장고", "에어컨");
        assertThat(existing.getCareers()).hasSize(2);
        assertThat(existing.getCareers()).extracting(ResumeCareer::getPeriod)
                .doesNotContain("2020");
    }

    @Test void rejectsUpdateWhenResumeNotFound() {
        when(resumes.findByUserId(200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateResume(request(), 200L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESUME_NOT_FOUND);
    }

    @Test void deletesOwnResume() {
        Resume existing = Resume.builder().userId(200L).build();
        when(resumes.findByUserId(200L)).thenReturn(Optional.of(existing));

        service.deleteResume(200L);

        verify(resumes).delete(existing);
    }

    @Test void rejectsDeleteWhenResumeNotFound() {
        when(resumes.findByUserId(200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteResume(200L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESUME_NOT_FOUND);

        verify(resumes, never()).delete(any());
    }

    @Test void getsResumeByUserIdForPublicView() {
        Resume existing = Resume.builder().userId(200L).build();
        existing.updateContent("제목", "소개", List.of("세탁기"), List.of());
        when(resumes.findByUserId(200L)).thenReturn(Optional.of(existing));

        ResumeResponseDto result = service.getResumeByUserId(200L);

        assertThat(result.userId()).isEqualTo(200L);
        assertThat(result.skills()).containsExactly("세탁기");
    }

    @Test void rejectsGetWhenResumeNotFound() {
        when(resumes.findByUserId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getResumeByUserId(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESUME_NOT_FOUND);
    }
}
