package uy.com.equipos.panelmanagement.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.com.equipos.panelmanagement.data.Answer;
import uy.com.equipos.panelmanagement.data.AnswerRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AnswerService {

    private final AnswerRepository answerRepository;

    @Autowired
    public AnswerService(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    public List<Answer> findAll() {
        return answerRepository.findAll();
    }

    public Optional<Answer> findById(Long id) {
        return answerRepository.findById(id);
    }

    public Answer save(Answer answer) {
        return answerRepository.save(answer);
    }

    public void deleteById(Long id) {
        answerRepository.deleteById(id);
    }

    public Optional<Answer> findBySurveyPanelistParticipationAndQuestionCode(
            uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation participation, String questionCode) {
        return answerRepository.findBySurveyPanelistParticipationAndQuestionCode(participation, questionCode);
    }
}
