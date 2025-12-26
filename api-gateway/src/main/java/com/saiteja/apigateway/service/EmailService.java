package com.saiteja.apigateway.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final Resend resend;
    private final String fromEmail;

    public EmailService(@Value("${resend.api.key}") String apiKey,
                       @Value("${resend.from.email:noreply@saiteja.ink}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    public void sendPasswordResetEmail(String email, String token, String resetUrl) {
        try {
            String resetLink = resetUrl + "?token=" + token;
            
            String htmlContent = buildPasswordResetEmailHtml(resetLink);
            
            CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(email)
                    .subject("Reset Your Password - Flight Management System")
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(emailOptions);
            logger.info("Password reset email sent successfully to {} with ID: {}", email, response.getId());
        } catch (ResendException e) {
            logger.error("Failed to send password reset email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String buildPasswordResetEmailHtml(String resetLink) {
        // Use String.format with proper escaping - replace % with %% to escape percentage signs in CSS
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reset Your Password</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                    <tr>
                        <td style="padding: 20px 0; text-align: center; background-color: #ffffff;">
                            <table role="presentation" style="width: 600px; margin: 0 auto; border-collapse: collapse; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: bold;">Password Reset Request</h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px; background-color: #ffffff;">
                                        <p style="margin: 0 0 20px 0; color: #333333; font-size: 16px; line-height: 1.6;">
                                            Hello,
                                        </p>
                                        <p style="margin: 0 0 20px 0; color: #333333; font-size: 16px; line-height: 1.6;">
                                            We received a request to reset your password for your Flight Management System account. 
                                            Click the button below to reset your password:
                                        </p>
                                        <table role="presentation" style="width: 100%%; margin: 30px 0;">
                                            <tr>
                                                <td style="text-align: center;">
                                                    <a href="%s" style="display: inline-block; padding: 14px 32px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;">Reset Password</a>
                                                </td>
                                            </tr>
                                        </table>
                                        <p style="margin: 20px 0 0 0; color: #666666; font-size: 14px; line-height: 1.6;">
                                            Or copy and paste this link into your browser:
                                        </p>
                                        <p style="margin: 10px 0 0 0; color: #667eea; font-size: 14px; word-break: break-all;">
                                            %s
                                        </p>
                                        <p style="margin: 30px 0 0 0; color: #999999; font-size: 12px; line-height: 1.6;">
                                            <strong>Important:</strong> This link will expire in 1 hour. If you didn't request a password reset, please ignore this email.
                                        </p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 20px 40px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; text-align: center;">
                                        <p style="margin: 0; color: #999999; font-size: 12px;">
                                            © 2024 Flight Management System. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;
        return String.format(template, resetLink, resetLink);
    }
}

