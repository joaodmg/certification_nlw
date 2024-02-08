package com.rocketseat.certification_nlw.modules.students.useCases;

import com.rocketseat.certification_nlw.modules.questions.entities.QuestionEntity;
import com.rocketseat.certification_nlw.modules.questions.repositories.QuestionRepository;
import com.rocketseat.certification_nlw.modules.students.dto.StudentCertificationAnswerDTO;
import com.rocketseat.certification_nlw.modules.students.dto.VerifyHasCertificationDTO;
import com.rocketseat.certification_nlw.modules.students.entities.AnswersCertificationsEntity;
import com.rocketseat.certification_nlw.modules.students.entities.CertificationStudentEntity;
import com.rocketseat.certification_nlw.modules.students.entities.StudentEntity;
import com.rocketseat.certification_nlw.modules.students.repositories.CertificationStudentRepository;
import com.rocketseat.certification_nlw.modules.students.repositories.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StudentCertificationAnswersUseCase {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private CertificationStudentRepository certificationStudentRepository;
    @Autowired
    private VerifyIfHasCertificationUseCase verifyIfHasCertificationUseCase;

    public CertificationStudentEntity execute(StudentCertificationAnswerDTO studentCertificationAnswerDTO) throws Exception {
        var hasCertification = this.verifyIfHasCertificationUseCase.execute(new VerifyHasCertificationDTO(
                studentCertificationAnswerDTO.getEmail(), studentCertificationAnswerDTO.getTechnology()
        ));

        if (hasCertification) {
            throw new Exception("Você já tirou sua certificação!");
        }

        List<QuestionEntity> questionEntities = questionRepository.findByTechnology(studentCertificationAnswerDTO.getTechnology());
        List<AnswersCertificationsEntity> answersCertificationsEntities = new ArrayList<>();

        AtomicInteger correctAnswers = new AtomicInteger(0);

        studentCertificationAnswerDTO.getQuestionAnswers().stream().forEach(questionAnswer -> {
            var questionEntity = questionEntities.stream().filter(question -> question.getId().equals(questionAnswer.getQuestionId())).
                    findFirst().get();

            var findCorrectAlternative = questionEntity.getAlternatives().stream().filter(alternative -> alternative.isCorrect()).
                    findFirst().get();

            if(findCorrectAlternative.getId().equals(questionAnswer.getAlternativeId())) {
                questionAnswer.setCorrect(true);
                correctAnswers.incrementAndGet();
            } else {
                questionAnswer.setCorrect(false);
            }

            var answersCertificationEntity = AnswersCertificationsEntity.builder()
                    .answerId(questionAnswer.getAlternativeId())
                    .questionId(questionAnswer.getQuestionId())
                    .isCorrect(questionAnswer.isCorrect()).build();

            answersCertificationsEntities.add(answersCertificationEntity);
        });

        var student = studentRepository.findByEmail(studentCertificationAnswerDTO.getEmail());
        UUID studentId;
        if (student.isEmpty()) {
            var studentCreated = StudentEntity.builder().email(studentCertificationAnswerDTO.getEmail()).build();
            studentCreated = studentRepository.save(studentCreated);
            studentId = studentCreated.getId();
        } else {
           studentId = student.get().getId();
        }

        CertificationStudentEntity certificationStudentEntity = CertificationStudentEntity.builder()
                .technology(studentCertificationAnswerDTO.getTechnology())
                .studentID(studentId)
                .grade(correctAnswers.get())
                .build();

        var certificationStudentCreated = certificationStudentRepository.save(certificationStudentEntity);

        answersCertificationsEntities.stream().forEach(answerCertification -> {
            answerCertification.setCertificationId(certificationStudentEntity.getId());
            answerCertification.setCertificationStudentEntity(certificationStudentEntity);
        });

        certificationStudentEntity.setAnswersCertificationsEntity(answersCertificationsEntities);

        certificationStudentRepository.save(certificationStudentEntity);

        return certificationStudentCreated;
    }
}
