package com.emailfilter.service;

import com.emailfilter.dto.ExtractedTask;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class RuleBasedExtractionServiceTest {

    private final RuleBasedExtractionService service = new RuleBasedExtractionService();

    @Test
    public void testHackathonExtraction() {
        String body = "Please register for the Upcoming Hackathon by tomorrow.";
        String subject = "Hackathon Alert";
        List<ExtractedTask> tasks = service.extractTasks(body, subject);
        
        assertFalse(tasks.isEmpty());
        assertEquals("register for the Upcoming Hackathon by tomorrow", tasks.get(0).getTask());
        assertEquals("tomorrow", tasks.get(0).getDeadline());
        assertEquals("HIGH", tasks.get(0).getPriority());
        assertEquals("Hackathon", tasks.get(0).getCategory());
    }

    @Test
    public void testInternshipDeadline() {
        String body = "The last date to apply for the Internship is 25-05-2026. Make sure to upload your resume.";
        String subject = "Internship Opportunity";
        List<ExtractedTask> tasks = service.extractTasks(body, subject);
        
        assertFalse(tasks.isEmpty());
        // Should find "apply for the Internship is 25-05-2026"
        assertTrue(tasks.stream().anyMatch(t -> t.getTask().contains("apply for the Internship")));
        assertTrue(tasks.stream().anyMatch(t -> "25-05-2026".equals(t.getDeadline())));
        assertTrue(tasks.stream().anyMatch(t -> "Internship".equals(t.getCategory())));
    }

    @Test
    public void testMandatoryAction() {
        String body = "It is mandatory to complete the assignment within 5 days.";
        String subject = "Urgent: Assignment";
        List<ExtractedTask> tasks = service.extractTasks(body, subject);
        
        assertFalse(tasks.isEmpty());
        assertEquals("HIGH", tasks.get(0).getPriority());
        assertEquals("Assignment", tasks.get(0).getCategory());
    }
}
