package com.successacademy.authservice.service.workflow;

import com.successacademy.authservice.dto.workflow.*;
import com.successacademy.authservice.model.entity.workflow.WorkflowDefinition;
import com.successacademy.authservice.model.entity.workflow.WorkflowState;
import com.successacademy.authservice.model.entity.workflow.WorkflowTransition;
import com.successacademy.authservice.repository.workflow.WorkflowDefinitionRepository;
import com.successacademy.authservice.repository.workflow.WorkflowStateRepository;
import com.successacademy.authservice.repository.workflow.WorkflowTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowStateRepository stateRepository;
    private final WorkflowTransitionRepository transitionRepository;

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionDto> getDefinitionsForTenant(String tenantId) {
        String cleanTenant = cleanTenant(tenantId);
        List<WorkflowDefinition> list = definitionRepository.findAvailableForTenant(cleanTenant);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowDefinitionDto getDefinitionById(Long id, String tenantId) {
        String cleanTenant = cleanTenant(tenantId);
        WorkflowDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found: " + id));

        // Tenant isolation check: allow system templates (tenantId null) or exact tenant match
        if (def.getTenantId() != null && !def.getTenantId().equalsIgnoreCase(cleanTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to cross-tenant workflow definition.");
        }

        return toDto(def);
    }

    @Transactional
    public WorkflowDefinitionDto createDraftDefinition(CreateWorkflowDefinitionRequest req, String tenantId, String actorUsername) {
        String cleanTenant = cleanTenant(tenantId);
        String wfKey = req.getWorkflowKey().trim().toUpperCase();

        Integer maxVer = definitionRepository.findMaxVersion(wfKey, cleanTenant);
        int nextVer = (maxVer != null ? maxVer : 0) + 1;

        WorkflowDefinition def = WorkflowDefinition.builder()
                .tenantId(cleanTenant)
                .workflowKey(wfKey)
                .name(req.getName().trim())
                .description(req.getDescription())
                .resourceType(req.getResourceType().trim().toUpperCase())
                .featureKey(req.getFeatureKey() != null ? req.getFeatureKey().trim().toUpperCase() : null)
                .status("DRAFT")
                .version(nextVer)
                .isSystem(false)
                .createdBy(actorUsername != null ? actorUsername : "ADMIN")
                .build();

        WorkflowDefinition savedDef = definitionRepository.save(def);

        // Save states
        if (req.getStates() != null) {
            for (WorkflowStateDto sDto : req.getStates()) {
                WorkflowState state = WorkflowState.builder()
                        .workflowDefinition(savedDef)
                        .stateKey(sDto.getStateKey().trim().toUpperCase())
                        .name(sDto.getName().trim())
                        .stateType(sDto.getStateType() != null ? sDto.getStateType().toUpperCase() : "INTERMEDIATE")
                        .displayOrder(sDto.getDisplayOrder() != null ? sDto.getDisplayOrder() : 0)
                        .isInitial(sDto.isInitial())
                        .isTerminal(sDto.isTerminal())
                        .build();
                stateRepository.save(state);
            }
        }

        // Save transitions
        if (req.getTransitions() != null) {
            for (WorkflowTransitionDto tDto : req.getTransitions()) {
                WorkflowTransition trans = WorkflowTransition.builder()
                        .workflowDefinition(savedDef)
                        .fromState(tDto.getFromState().trim().toUpperCase())
                        .toState(tDto.getToState().trim().toUpperCase())
                        .actionKey(tDto.getActionKey().trim().toUpperCase())
                        .name(tDto.getName().trim())
                        .permissionKey(tDto.getPermissionKey().trim())
                        .requiredScope(tDto.getRequiredScope() != null ? tDto.getRequiredScope().toUpperCase() : "TENANT")
                        .approvalRequired(tDto.isApprovalRequired())
                        .approvalLevel(tDto.getApprovalLevel() != null ? tDto.getApprovalLevel() : 1)
                        .makerCheckerRequired(tDto.isMakerCheckerRequired())
                        .requiresComment(tDto.isRequiresComment())
                        .requiresRejectionReason(tDto.isRequiresRejectionReason())
                        .isActive(tDto.isActive())
                        .build();
                transitionRepository.save(trans);
            }
        }

        log.info("Created draft workflow definition '{}' v{} for tenant '{}'", wfKey, nextVer, cleanTenant);
        return toDto(definitionRepository.findById(savedDef.getId()).orElse(savedDef));
    }

    @Transactional
    public WorkflowDefinitionDto updateDraftDefinition(Long id, UpdateWorkflowDefinitionRequest req, String tenantId, String actorUsername) {
        String cleanTenant = cleanTenant(tenantId);
        WorkflowDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found: " + id));

        // Tenant isolation
        if (def.getTenantId() != null && !def.getTenantId().equalsIgnoreCase(cleanTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to cross-tenant workflow definition.");
        }

        // Protected system workflows cannot be modified directly
        if (def.isSystem() || def.getTenantId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "System workflow definitions are immutable templates. Create a tenant-specific version instead.");
        }

        // Immutable published versions
        if ("PUBLISHED".equalsIgnoreCase(def.getStatus()) || "ARCHIVED".equalsIgnoreCase(def.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Published or archived workflow versions are immutable. Create a new version instead.");
        }

        def.setName(req.getName().trim());
        def.setDescription(req.getDescription());
        if (req.getResourceType() != null) def.setResourceType(req.getResourceType().trim().toUpperCase());
        if (req.getFeatureKey() != null) def.setFeatureKey(req.getFeatureKey().trim().toUpperCase());
        def.setUpdatedAt(LocalDateTime.now());

        stateRepository.deleteByWorkflowDefinitionId(def.getId());
        transitionRepository.deleteByWorkflowDefinitionId(def.getId());

        if (req.getStates() != null) {
            for (WorkflowStateDto sDto : req.getStates()) {
                WorkflowState state = WorkflowState.builder()
                        .workflowDefinition(def)
                        .stateKey(sDto.getStateKey().trim().toUpperCase())
                        .name(sDto.getName().trim())
                        .stateType(sDto.getStateType() != null ? sDto.getStateType().toUpperCase() : "INTERMEDIATE")
                        .displayOrder(sDto.getDisplayOrder() != null ? sDto.getDisplayOrder() : 0)
                        .isInitial(sDto.isInitial())
                        .isTerminal(sDto.isTerminal())
                        .build();
                stateRepository.save(state);
            }
        }

        if (req.getTransitions() != null) {
            for (WorkflowTransitionDto tDto : req.getTransitions()) {
                WorkflowTransition trans = WorkflowTransition.builder()
                        .workflowDefinition(def)
                        .fromState(tDto.getFromState().trim().toUpperCase())
                        .toState(tDto.getToState().trim().toUpperCase())
                        .actionKey(tDto.getActionKey().trim().toUpperCase())
                        .name(tDto.getName().trim())
                        .permissionKey(tDto.getPermissionKey().trim())
                        .requiredScope(tDto.getRequiredScope() != null ? tDto.getRequiredScope().toUpperCase() : "TENANT")
                        .approvalRequired(tDto.isApprovalRequired())
                        .approvalLevel(tDto.getApprovalLevel() != null ? tDto.getApprovalLevel() : 1)
                        .makerCheckerRequired(tDto.isMakerCheckerRequired())
                        .requiresComment(tDto.isRequiresComment())
                        .requiresRejectionReason(tDto.isRequiresRejectionReason())
                        .isActive(tDto.isActive())
                        .build();
                transitionRepository.save(trans);
            }
        }

        WorkflowDefinition updated = definitionRepository.save(def);
        log.info("Updated draft workflow definition '{}' id {}", def.getWorkflowKey(), id);
        return toDto(updated);
    }

    @Transactional
    public WorkflowDefinitionDto createNextVersion(Long sourceId, String tenantId, String actorUsername) {
        String cleanTenant = cleanTenant(tenantId);
        WorkflowDefinition source = definitionRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source workflow definition not found: " + sourceId));

        String wfKey = source.getWorkflowKey();
        Integer maxVer = definitionRepository.findMaxVersion(wfKey, cleanTenant);
        int nextVer = (maxVer != null ? maxVer : source.getVersion()) + 1;

        WorkflowDefinition newDef = WorkflowDefinition.builder()
                .tenantId(cleanTenant)
                .workflowKey(wfKey)
                .name(source.getName())
                .description(source.getDescription())
                .resourceType(source.getResourceType())
                .featureKey(source.getFeatureKey())
                .status("DRAFT")
                .version(nextVer)
                .isSystem(false)
                .createdBy(actorUsername != null ? actorUsername : "ADMIN")
                .build();

        WorkflowDefinition saved = definitionRepository.save(newDef);

        // Copy states
        List<WorkflowState> states = stateRepository.findByWorkflowDefinitionIdOrderByDisplayOrderAsc(sourceId);
        for (WorkflowState s : states) {
            WorkflowState copy = WorkflowState.builder()
                    .workflowDefinition(saved)
                    .stateKey(s.getStateKey())
                    .name(s.getName())
                    .stateType(s.getStateType())
                    .displayOrder(s.getDisplayOrder())
                    .isInitial(s.isInitial())
                    .isTerminal(s.isTerminal())
                    .build();
            stateRepository.save(copy);
        }

        // Copy transitions
        List<WorkflowTransition> transitions = transitionRepository.findByWorkflowDefinitionId(sourceId);
        for (WorkflowTransition t : transitions) {
            WorkflowTransition copy = WorkflowTransition.builder()
                    .workflowDefinition(saved)
                    .fromState(t.getFromState())
                    .toState(t.getToState())
                    .actionKey(t.getActionKey())
                    .name(t.getName())
                    .permissionKey(t.getPermissionKey())
                    .requiredScope(t.getRequiredScope())
                    .approvalRequired(t.isApprovalRequired())
                    .approvalLevel(t.getApprovalLevel())
                    .makerCheckerRequired(t.isMakerCheckerRequired())
                    .requiresComment(t.isRequiresComment())
                    .requiresRejectionReason(t.isRequiresRejectionReason())
                    .isActive(t.isActive())
                    .build();
            transitionRepository.save(copy);
        }

        log.info("Created new version {} for workflow '{}' (based on id {})", nextVer, wfKey, sourceId);
        return toDto(definitionRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public WorkflowDefinitionDto publishDefinition(Long id, String tenantId, String actorUsername) {
        String cleanTenant = cleanTenant(tenantId);
        WorkflowDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found: " + id));

        if (def.getTenantId() != null && !def.getTenantId().equalsIgnoreCase(cleanTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to cross-tenant workflow definition.");
        }

        if (def.isSystem() && def.getTenantId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "System templates cannot be re-published.");
        }

        // Validate that an initial state exists
        List<WorkflowState> states = stateRepository.findByWorkflowDefinitionIdOrderByDisplayOrderAsc(id);
        boolean hasInitial = states.stream().anyMatch(WorkflowState::isInitial);
        if (!hasInitial) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow definition must have at least one initial state before publishing.");
        }

        // Validate transitions exist
        List<WorkflowTransition> transitions = transitionRepository.findByWorkflowDefinitionId(id);
        if (transitions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow definition must have at least one transition before publishing.");
        }

        // Archive previous published versions for this tenant and workflow key
        List<WorkflowDefinition> existingPub = definitionRepository.findPublishedDefinitionsForTenant(cleanTenant, def.getWorkflowKey());
        for (WorkflowDefinition ep : existingPub) {
            if (cleanTenant.equalsIgnoreCase(ep.getTenantId()) && !ep.getId().equals(def.getId())) {
                ep.setStatus("ARCHIVED");
                ep.setUpdatedAt(LocalDateTime.now());
                definitionRepository.save(ep);
            }
        }

        def.setStatus("PUBLISHED");
        def.setUpdatedAt(LocalDateTime.now());
        WorkflowDefinition saved = definitionRepository.save(def);

        log.info("Published workflow definition '{}' v{} for tenant '{}'", def.getWorkflowKey(), def.getVersion(), cleanTenant);
        return toDto(saved);
    }

    @Transactional
    public WorkflowDefinitionDto archiveDefinition(Long id, String tenantId, String actorUsername) {
        String cleanTenant = cleanTenant(tenantId);
        WorkflowDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found: " + id));

        if (def.getTenantId() != null && !def.getTenantId().equalsIgnoreCase(cleanTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to cross-tenant workflow definition.");
        }

        if (def.isSystem() && def.getTenantId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "System templates cannot be archived.");
        }

        def.setStatus("ARCHIVED");
        def.setUpdatedAt(LocalDateTime.now());
        WorkflowDefinition saved = definitionRepository.save(def);

        log.info("Archived workflow definition '{}' v{} for tenant '{}'", def.getWorkflowKey(), def.getVersion(), cleanTenant);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public WorkflowDefinition findEffectiveDefinition(String tenantId, String workflowKey) {
        String cleanTenant = cleanTenant(tenantId);
        List<WorkflowDefinition> candidates = definitionRepository.findPublishedDefinitionsForTenant(cleanTenant, workflowKey.trim().toUpperCase());
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active published workflow definition found for key: " + workflowKey);
        }
        return candidates.get(0); // Ordered: tenant override first, then highest version
    }

    public WorkflowDefinitionDto toDto(WorkflowDefinition def) {
        List<WorkflowState> states = stateRepository.findByWorkflowDefinitionIdOrderByDisplayOrderAsc(def.getId());
        List<WorkflowTransition> transitions = transitionRepository.findByWorkflowDefinitionId(def.getId());

        return WorkflowDefinitionDto.builder()
                .id(def.getId())
                .tenantId(def.getTenantId())
                .workflowKey(def.getWorkflowKey())
                .name(def.getName())
                .description(def.getDescription())
                .resourceType(def.getResourceType())
                .featureKey(def.getFeatureKey())
                .status(def.getStatus())
                .version(def.getVersion())
                .isSystem(def.isSystem())
                .createdBy(def.getCreatedBy())
                .createdAt(def.getCreatedAt())
                .updatedAt(def.getUpdatedAt())
                .states(states.stream().map(s -> WorkflowStateDto.builder()
                        .id(s.getId())
                        .stateKey(s.getStateKey())
                        .name(s.getName())
                        .stateType(s.getStateType())
                        .displayOrder(s.getDisplayOrder())
                        .isInitial(s.isInitial())
                        .isTerminal(s.isTerminal())
                        .build()).collect(Collectors.toList()))
                .transitions(transitions.stream().map(t -> WorkflowTransitionDto.builder()
                        .id(t.getId())
                        .fromState(t.getFromState())
                        .toState(t.getToState())
                        .actionKey(t.getActionKey())
                        .name(t.getName())
                        .permissionKey(t.getPermissionKey())
                        .requiredScope(t.getRequiredScope())
                        .approvalRequired(t.isApprovalRequired())
                        .approvalLevel(t.getApprovalLevel())
                        .makerCheckerRequired(t.isMakerCheckerRequired())
                        .requiresComment(t.isRequiresComment())
                        .requiresRejectionReason(t.isRequiresRejectionReason())
                        .isActive(t.isActive())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private String cleanTenant(String tenantId) {
        return (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
    }
}
