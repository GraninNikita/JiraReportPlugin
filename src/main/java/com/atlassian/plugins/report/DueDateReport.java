package com.atlassian.plugins.report;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.search.ReaderCache;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.query.Query;
import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

@Scanned
public class DueDateReport extends AbstractReport {

    private static final Logger log = Logger.getLogger(DueDateReport.class);

    @JiraImport
    private final SearchProvider searchProvider;
    @JiraImport
    private final CustomFieldManager customFieldManager;
    @JiraImport
    private final SearchService searchService;
    @JiraImport
    private final FieldVisibilityManager fieldVisibilityManager;
    @JiraImport
    private final ProjectManager projectManager;
    private final DateTimeFormatter formatter;
    private JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
    private GroupManager groupManager;
    private Date dueDate;
    private Long projectId;
    private static ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal.withInitial(() ->
            new SimpleDateFormat("yyyy-MM-dd"));

    public DueDateReport(SearchProvider searchProvider,
                         CustomFieldManager customFieldManager,
                         SearchService searchService,
                         FieldVisibilityManager fieldVisibilityManager,
                         ProjectManager projectManager,
                         @JiraImport DateTimeFormatterFactory dateTimeFormatterFactory
    ) {
        this.searchProvider = searchProvider;
        this.customFieldManager = customFieldManager;
        this.searchService = searchService;
        this.fieldVisibilityManager = fieldVisibilityManager;
        this.projectManager = projectManager;
        this.formatter = dateTimeFormatterFactory.formatter().withStyle(DateTimeStyle.DATE).forLoggedInUser();
        this.groupManager = ComponentManager.getComponentInstanceOfType(GroupManager.class);
    }

    public String generateReportHtml(ProjectActionSupport action, Map params) throws Exception {
        String projectKey = projectManager.getProjectObj(projectId).getKey();
        List<Issue> issues = findIssues(dueDate, projectKey);

        final Map startingParams = ImmutableMap.builder()
                .put("action", action)
                .put("customFieldManager", customFieldManager)
                .put("fieldVisibility", fieldVisibilityManager)
                .put("searchService", searchService)
                .put("portlet", this)
                .put("formatter", formatter)
                .put("dueDate", dueDate)
                .put("issues", issues)
                .build();

        return descriptor.getHtml("view", startingParams);
    }

    public List<Issue> findIssues(Date dueDate, String projectKey) throws SearchException, JqlParseException {
        String dueDateFormatted = dateFormat.get().format(dueDate);
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
        JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        Query query = queryBuilder.where()
                .dueBetween(null, dueDateFormatted)
                .and()
                .project(projectKey)
                .buildQuery();
        SearchResults results = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter());

        return results.getIssues().size() > 0 ? results.getIssues() : Collections.emptyList();
    }

    public void validate(ProjectActionSupport action, Map params) {
        String dueDateParam = ParameterUtils.getStringParam(params, "dueDate");

        try {
            if (dueDateParam == null || "".equals(dueDateParam)) dueDate = new Date();
            else
                dueDate = formatter.parse(dueDateParam);
        } catch (IllegalArgumentException e) {
            action.addError("dueDate", action.getText("due-date-report.dueDate.required"));
            log.error("Exception while parsing dueDate");
        }
        projectId = ParameterUtils.getLongParam(params, "selectedProjectId");

        if (projectId == null || projectManager.getProjectObj(projectId) == null) {
            action.addError("selectedProjectId", action.getText("due-date-report.projectid.invalid"));
            log.error("Invalid projectId");
        }
    }

    @Override
    public boolean showReport() {
        return isReportAvailable();
    }

    private boolean isReportAvailable() {
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
        return groupManager.isUserInGroup(user, Constants.PROJECT_MANAGER_ROLE_NAME);
    }

}
