package uy.com.equipos.panelmanagement.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uy.com.equipos.panelmanagement.services.dtos.AlchemerContactDto;
import uy.com.equipos.panelmanagement.services.dtos.AlchemerSurveyContactResponseDto;

@Service
public class AlchemerService {

    private static final Logger log = LoggerFactory.getLogger(AlchemerService.class);

    private final RestTemplate restTemplate;

    @Value("${alchemer.api.token}")
    private String apiToken;

    @Value("${alchemer.api.token.secret}")
    private String apiTokenSecret;

    private static final String ALCHEMER_API_BASE_URL = "https://api.alchemer.com";

    public AlchemerService() {
        this.restTemplate = new RestTemplate();
    }

    public List<AlchemerContactDto> getSurveyContacts(String alchemerSurveyId) {
        String surveyId = extractSurveyId(alchemerSurveyId);
        String campaignId = extractCampaignId(alchemerSurveyId);

        if (surveyId == null || campaignId == null) {
            log.error("Could not extract surveyId and/or campaignId from alchemerSurveyId: {}", alchemerSurveyId);
            return Collections.emptyList();
        }

        List<AlchemerContactDto> allContacts = new ArrayList<>();
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
                    .pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "surveycontact")
                    .queryParam("_method", "GET")
                    .queryParam("api_token", apiToken)
                    .queryParam("api_token_secret", apiTokenSecret)
                    .queryParam("page", page)
                    .queryParam("resultsperpage", 50);

            String url = builder.toUriString();
            log.debug("Fetching contacts from URL: {}", url);

            try {
                ResponseEntity<AlchemerSurveyContactResponseDto> response = restTemplate.getForEntity(url, AlchemerSurveyContactResponseDto.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && response.getBody().isResultOk()) {
                    AlchemerSurveyContactResponseDto responseBody = response.getBody();
                    if (responseBody.getData() != null) {
                        allContacts.addAll(responseBody.getData());
                    }
                    totalPages = responseBody.getTotalPages();
                    page++;
                } else {
                    log.error("Failed to fetch contacts from Alchemer API. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                    break;
                }
            } catch (HttpClientErrorException e) {
                log.error("Error while fetching contacts from Alchemer API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                break;
            } catch (Exception e) {
                log.error("An unexpected error occurred while fetching contacts from Alchemer API", e);
                break;
            }
        }

        return allContacts;
    }

    public String extractCampaignId(String surveyLink) {
		if (surveyLink == null) {
			return null;
		}

		// Pattern for the new URL format:
		// https://app.alchemer.com/invite/messages/id/SURVEY_ID/link/CAMPAIGN_ID
		// We are interested in the CAMPAIGN_ID, which is the last numeric part.
		// Example: "https://app.alchemer.com/invite/messages/id/8367882/link/24099873"
		// -> "24099873"
		Pattern newUrlPattern = Pattern.compile("/id/(\\d+)/link/(\\d+)$");
		Matcher matcher = newUrlPattern.matcher(surveyLink);

		if (matcher.find()) {
			// Group 2 is the CAMPAIGN_ID
			return matcher.group(2);
		}

		// Fallback to existing logic for other URL formats or direct IDs
		if (surveyLink.contains("/")) {
			String[] parts = surveyLink.split("/");
			for (int i = parts.length - 1; i >= 0; i--) {
				// Check if the part is purely numeric and not empty
				if (!parts[i].isEmpty() && parts[i].matches("\\d+")) {
					return parts[i];
				}
			}
		}
		// If not a recognized URL pattern or no numeric part found by fallback,
		// and it's not null and doesn't contain '/', assume it's the ID directly.
		// Also, if it contained '/' but no numeric part was extracted, it might be a
		// malformed URL
		// or a direct ID with an accidental slash. If it's numeric, treat as ID.
		if (surveyLink.matches("\\d+")) {
			return surveyLink;
		}
		// If no ID could be parsed, return null.
		return null;
	}

	public String extractSurveyId(String surveyLink) {
		if (surveyLink == null) {
			return null;
		}

		// 1. Try the new URL pattern:
		// https://app.alchemer.com/invite/messages/id/SURVEY_ID/link/CAMPAIGN_ID
		// Example: "https://app.alchemer.com/invite/messages/id/8367882/link/24099873"
		// -> "8367882"
		Pattern newUrlPattern = Pattern.compile("/id/(\\d+)/link/(\\d+)");
		Matcher newMatcher = newUrlPattern.matcher(surveyLink);
		if (newMatcher.find()) {
			return newMatcher.group(1); // SURVEY_ID
		}

		// 2. Try common older Alchemer URL pattern: e.g., /s3/SURVEY_ID/... or
		// /survey/SURVEY_ID/...
		// Example: "https://app.alchemer.com/s3/1234567/My-Survey" -> "1234567"
		Pattern oldUrlPattern = Pattern.compile("/(?:s3|survey)/(\\d+)");
		Matcher oldMatcher = oldUrlPattern.matcher(surveyLink);
		if (oldMatcher.find()) {
			return oldMatcher.group(1); // SURVEY_ID
		}

		// 3. Check if the surveyLink itself is a plain numeric ID
		// Example: "8367882" -> "8367882"
		if (surveyLink.matches("\\d+")) {
			return surveyLink;
		}

		// 4. If no pattern matched and it's not a plain number, we couldn't extract a
		// survey ID.
		return null;
	}

}
