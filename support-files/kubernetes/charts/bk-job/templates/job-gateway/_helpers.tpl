{{/*
Return the job-gateway Service enabled
判断条件：
1.job-gateway模块开启
2.全链路灰度环境下，基础泳道才部署Service
*/}}
{{- define "job.gateway.service.enabled" -}}
{{- if (not .Values.gatewayConfig.enabled) -}}
{{- false -}}
{{- else if (ne .Values.laneType "base") -}}
{{- false -}}
{{- else -}}
{{- true -}}
{{- end -}}
{{- end -}}
