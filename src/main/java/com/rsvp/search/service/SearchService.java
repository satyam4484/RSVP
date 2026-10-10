package com.rsvp.search.service;

import com.rsvp.common.enums.ContactType;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.util.SearchText;
import com.rsvp.contact.repository.ContactRepository;
import com.rsvp.document.repository.DocumentRepository;
import com.rsvp.event.repository.EventRepository;
import com.rsvp.event.repository.EventSpecifications;
import com.rsvp.search.dto.SearchResponse;
import com.rsvp.search.dto.SearchResponse.SearchHit;
import com.rsvp.task.repository.TaskRepository;
import com.rsvp.travel.repository.TravelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

	private final EventRepository eventRepository;
	private final ContactRepository contactRepository;
	private final TaskRepository taskRepository;
	private final TravelRepository travelRepository;
	private final DocumentRepository documentRepository;

	@Transactional(readOnly = true)
	public SearchResponse search(Long userId, String query) {
		if (query == null || query.trim().length() < 2) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Search query must be at least 2 characters");
		}
		String like = SearchText.like(query);
		var page = PageRequest.of(0, 20);
		List<SearchHit> events = eventRepository.findAll(EventSpecifications.filter(userId, null, null, null, query.trim()), page)
				.map(event -> new SearchHit(event.getUuid(), event.getName(), event.getLocationName())).getContent();
		List<SearchHit> contacts = contactRepository.search(userId, null, like, page).map(contact -> new SearchHit(contact.getUuid(),
				contact.getName(), contact.getCompanyName())).getContent();
		List<SearchHit> vendors = contactRepository.search(userId, ContactType.VENDOR, like, page)
				.map(contact -> new SearchHit(contact.getUuid(), contact.getName(), contact.getCompanyName())).getContent();
		List<SearchHit> tasks = taskRepository.search(userId, like, page).stream()
				.map(task -> new SearchHit(task.getUuid(), task.getTitle(), task.getStatus().name())).toList();
		List<SearchHit> travel = travelRepository.search(userId, like, page).stream()
				.map(item -> new SearchHit(item.getUuid(), item.getTitle(), item.getType().name())).toList();
		List<SearchHit> documents = documentRepository.search(userId, like, page).stream()
				.map(document -> new SearchHit(document.getUuid(), document.getName(), document.getStorageProvider().name())).toList();
		return new SearchResponse(events, contacts, vendors, tasks, travel, documents);
	}
}
