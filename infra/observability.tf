# ── CloudWatch Alarms ─────────────────────────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "alb_5xx" {
  alarm_name          = "${var.app_name}-alb-5xx-rate"
  alarm_description   = "ALB 5xx errors exceed threshold — possible application crash or overload"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  threshold           = 10
  treat_missing_data  = "notBreaching"

  metric_query {
    id          = "error_rate"
    expression  = "errors / MAX([errors, requests]) * 100"
    label       = "5xx Error Rate (%)"
    return_data = true
  }

  metric_query {
    id = "errors"
    metric {
      namespace   = "AWS/ApplicationELB"
      metric_name = "HTTPCode_ELB_5XX_Count"
      period      = 60
      stat        = "Sum"
      dimensions = {
        LoadBalancer = aws_lb.main.arn_suffix
      }
    }
  }

  metric_query {
    id = "requests"
    metric {
      namespace   = "AWS/ApplicationELB"
      metric_name = "RequestCount"
      period      = 60
      stat        = "Sum"
      dimensions = {
        LoadBalancer = aws_lb.main.arn_suffix
      }
    }
  }

  tags = { Name = "${var.app_name}-alb-5xx-alarm" }
}

resource "aws_cloudwatch_metric_alarm" "ecs_cpu_high" {
  alarm_name          = "${var.app_name}-ecs-cpu-high"
  alarm_description   = "ECS CPU utilisation above 80% — auto-scaling may not keep up"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  threshold           = 80
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/ECS"
  metric_name = "CPUUtilization"
  period      = 60
  statistic   = "Average"

  dimensions = {
    ClusterName = aws_ecs_cluster.main.name
    ServiceName = aws_ecs_service.app.name
  }

  tags = { Name = "${var.app_name}-ecs-cpu-alarm" }
}

resource "aws_cloudwatch_metric_alarm" "rds_connections_high" {
  alarm_name          = "${var.app_name}-rds-connections-high"
  alarm_description   = "RDS connection count is high — check RDS Proxy pool or connection leaks"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  threshold           = 80
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/RDS"
  metric_name = "DatabaseConnections"
  period      = 60
  statistic   = "Average"

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.postgres.identifier
  }

  tags = { Name = "${var.app_name}-rds-connections-alarm" }
}

# ── CloudWatch Dashboard ──────────────────────────────────────────────────────

resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = var.app_name

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0; y = 0; width = 12; height = 6
        properties = {
          title  = "ALB Request Volume & 5xx Errors"
          region = var.aws_region
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_lb.main.arn_suffix,
              { stat = "Sum", period = 60, label = "Requests" }],
            ["AWS/ApplicationELB", "HTTPCode_ELB_5XX_Count", "LoadBalancer", aws_lb.main.arn_suffix,
              { stat = "Sum", period = 60, label = "5xx Errors", color = "#d62728" }],
          ]
          view = "timeSeries"
        }
      },
      {
        type   = "metric"
        x      = 12; y = 0; width = 12; height = 6
        properties = {
          title  = "ALB Latency (p50 / p99)"
          region = var.aws_region
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.main.arn_suffix,
              { stat = "p50", period = 60, label = "p50" }],
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.main.arn_suffix,
              { stat = "p99", period = 60, label = "p99", color = "#ff7f0e" }],
          ]
          view = "timeSeries"
        }
      },
      {
        type   = "metric"
        x      = 0; y = 6; width = 12; height = 6
        properties = {
          title  = "ECS CPU & Memory Utilisation"
          region = var.aws_region
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.main.name,
              "ServiceName", aws_ecs_service.app.name,
              { stat = "Average", period = 60, label = "CPU %" }],
            ["AWS/ECS", "MemoryUtilization", "ClusterName", aws_ecs_cluster.main.name,
              "ServiceName", aws_ecs_service.app.name,
              { stat = "Average", period = 60, label = "Memory %" }],
          ]
          view = "timeSeries"
        }
      },
      {
        type   = "metric"
        x      = 12; y = 6; width = 12; height = 6
        properties = {
          title  = "RDS Connections & ElastiCache Memory"
          region = var.aws_region
          metrics = [
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.postgres.identifier,
              { stat = "Average", period = 60, label = "DB Connections" }],
            ["AWS/ElastiCache", "DatabaseMemoryUsagePercentage",
              "ReplicationGroupId", aws_elasticache_replication_group.redis.replication_group_id,
              { stat = "Average", period = 60, label = "Redis Memory %" }],
          ]
          view = "timeSeries"
        }
      },
    ]
  })
}
