{{/*
Return the job-file-worker Service enabled
判断条件：
1.job-file-worker模块开启
2.全链路灰度环境下，基础泳道才部署Service
*/}}
{{- define "job.fileWorker.service.enabled" -}}
{{- if (not .Values.fileWorkerConfig.enabled) -}}
{{- false -}}
{{- else if (ne .Values.laneType "base") -}}
{{- false -}}
{{- else -}}
{{- true -}}
{{- end -}}
{{- end -}}
