# ── Application Load Balancer ────────────────────────────────────────────────

resource "aws_lb" "main" {
  name               = var.app_name
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  # Shield Standard is automatically active on all ALBs at no extra cost.
  # WAF association below provides the rate-based edge rule.
  enable_deletion_protection = true

  tags = { Name = var.app_name }
}

resource "aws_lb_target_group" "app" {
  name        = var.app_name
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"   # required for Fargate awsvpc networking

  health_check {
    path                = "/actuator/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
    matcher             = "200"
  }

  tags = { Name = var.app_name }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

# ── WAF v2 ───────────────────────────────────────────────────────────────────
# Rate-based rule: blocks IPs exceeding var.waf_rate_limit requests per 5 minutes.
# Pairs with the app-layer Bucket4j per-tenant limit to provide two tiers of
# rate protection — edge (per-IP) and application (per-org).

resource "aws_wafv2_web_acl" "main" {
  name  = "${var.app_name}-waf"
  scope = "REGIONAL"

  default_action {
    allow {}
  }

  rule {
    name     = "IpRateLimit"
    priority = 1

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = var.waf_rate_limit
        aggregate_key_type = "IP"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${var.app_name}-ip-rate-limit"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 2

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${var.app_name}-common-rules"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${var.app_name}-waf"
    sampled_requests_enabled   = true
  }

  tags = { Name = "${var.app_name}-waf" }
}

resource "aws_wafv2_web_acl_association" "main" {
  resource_arn = aws_lb.main.arn
  web_acl_arn  = aws_wafv2_web_acl.main.arn
}
