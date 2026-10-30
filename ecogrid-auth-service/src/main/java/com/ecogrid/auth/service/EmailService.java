package com.ecogrid.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:lankasrividya08@gmail.com}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otpCode, String purposeLabel) {
        String subject = "EcoGrid P2P Verification Code";

        String htmlContent = buildOtpEmailTemplate(otpCode, purposeLabel);

        // Always print to console for development, testing and auditing
        System.out.println("======================================================================");
        System.out.println("[ECOGRID EMAIL NOTIFICATION] To: " + toEmail);
        System.out.println("[ECOGRID OTP CODE]: " + otpCode + " (" + purposeLabel + ")");
        System.out.println("Valid for 5 minutes. If no live SMTP configured, use this code.");
        System.out.println("======================================================================");

        if (mailSender != null && fromEmail != null && !fromEmail.isBlank() && !fromEmail.contains("${")) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                mailSender.send(message);
                log.info("OTP verification email sent successfully to: {}", toEmail);
            } catch (Exception ex) {
                log.warn("Could not dispatch live SMTP email to {} (cause: {}). Console OTP log remains active.",
                        toEmail, ex.getMessage());
            }
        } else {
            log.info("Live SMTP not configured. OTP printed to system console for: {}", toEmail);
        }
    }

    private String buildOtpEmailTemplate(String otpCode, String purposeLabel) {
        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"utf-8\">" +
            "<style>" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 24px; }" +
            ".container { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }" +
            ".header { background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding: 28px 24px; text-align: center; }" +
            ".brand-title { color: #ffffff; font-size: 22px; font-weight: 800; margin: 0; letter-spacing: -0.5px; }" +
            ".brand-subtitle { color: #38bdf8; font-size: 13px; font-weight: 600; margin-top: 4px; text-transform: uppercase; letter-spacing: 1px; }" +
            ".body { padding: 32px 28px; text-align: center; }" +
            ".greeting { font-size: 15px; color: #334155; margin-bottom: 20px; line-height: 1.5; }" +
            ".otp-box { background: #f0fdf4; border: 2px dashed #22c55e; border-radius: 10px; padding: 18px 24px; display: inline-block; margin: 16px 0; }" +
            ".otp-code { font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #15803d; margin: 0; font-family: monospace; }" +
            ".validity { color: #64748b; font-size: 13px; margin-top: 14px; font-weight: 500; }" +
            ".divider { height: 1px; background: #e2e8f0; margin: 24px 0; }" +
            ".footer { font-size: 12px; color: #94a3b8; line-height: 1.5; }" +
            "</style></head><body>" +
            "<div class=\"container\">" +
            "<div class=\"header\"><h1 class=\"brand-title\">EcoGrid P2P</h1><div class=\"brand-subtitle\">Renewable Energy Network</div></div>" +
            "<div class=\"body\">" +
            "<p class=\"greeting\">Your EcoGrid P2P verification code for <strong>" + purposeLabel + "</strong> is:</p>" +
            "<div class=\"otp-box\"><div class=\"otp-code\">" + otpCode + "</div></div>" +
            "<p class=\"validity\">This OTP is valid for <strong>5 minutes</strong>.</p>" +
            "<div class=\"divider\"></div>" +
            "<p class=\"footer\">If you did not request this code, please ignore this email.<br>&copy; 2026 EcoGrid P2P. Clean Energy Trading.</p>" +
            "</div></div></body></html>";
    }
}
