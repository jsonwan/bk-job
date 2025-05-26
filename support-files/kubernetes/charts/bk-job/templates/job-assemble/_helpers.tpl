{{/*
Return the job-assemble Service enabled
判断条件：
1.轻量化部署模式才部署Service
2.全链路灰度环境下，基础泳道才部署Service
*/}}
{{- define "job.assemble.service.enabled" -}}
{{- if (ne .Values.deploy.mode "lite") -}}
{{- false -}}
{{- else if (ne .Values.laneType "base") -}}
{{- false -}}
{{- else -}}
{{- true -}}
{{- end -}}
{{- end -}}
