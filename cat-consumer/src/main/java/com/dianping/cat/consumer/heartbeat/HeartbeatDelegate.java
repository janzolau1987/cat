package com.dianping.cat.consumer.heartbeat;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.configuration.ServerConfigManager;
import com.dianping.cat.consumer.heartbeat.model.entity.HeartbeatReport;
import com.dianping.cat.consumer.heartbeat.model.transform.DefaultNativeBuilder;
import com.dianping.cat.consumer.heartbeat.model.transform.DefaultNativeParser;
import com.dianping.cat.consumer.heartbeat.model.transform.DefaultSaxParser;
import com.dianping.cat.service.ReportDelegate;
import com.dianping.cat.task.TaskManager;
import com.dianping.cat.task.TaskManager.TaskProlicy;

public class HeartbeatDelegate implements ReportDelegate<HeartbeatReport> {

	@Inject
	private TaskManager m_taskManager;

	@Inject
	private ServerConfigManager m_configManager;

	@Override
	public void afterLoad(Map<String, HeartbeatReport> reports) {
	}

	@Override
	public void beforeSave(Map<String, HeartbeatReport> reports) {
		for (HeartbeatReport report : reports.values()) {
			Set<String> domainNames = report.getDomainNames();

			domainNames.clear();
			domainNames.addAll(reports.keySet());
		}
	}

	@Override
	public byte[] buildBinary(HeartbeatReport report) {
		return DefaultNativeBuilder.build(report);
	}

	@Override
	public String buildXml(HeartbeatReport report) {
		return report.toString();
	}

	@Override
	public boolean createHourlyTask(HeartbeatReport report) {
		String domain = report.getDomain();

		if (m_configManager.validateDomain(domain)) {
			return m_taskManager.createTask(report.getStartTime(), domain, HeartbeatAnalyzer.ID, TaskProlicy.DAILY);
		} else {
			return true;
		}
	}

	@Override
	public String getDomain(HeartbeatReport report) {
		return report.getDomain();
	}

	@Override
	public HeartbeatReport makeReport(String domain, long startTime, long duration) {
		HeartbeatReport report = new HeartbeatReport(domain);

		report.setStartTime(new Date(startTime));
		report.setEndTime(new Date(startTime + duration - 1));

		return report;
	}

	@Override
	public HeartbeatReport mergeReport(HeartbeatReport old, HeartbeatReport other) {
		HeartbeatReportMerger merger = new HeartbeatReportMerger(old);

		other.accept(merger);
		return old;
	}

	@Override
	public HeartbeatReport parseBinary(byte[] bytes) {
		return DefaultNativeParser.parse(bytes);
	}

	@Override
	public HeartbeatReport parseXml(String xml) throws Exception {
		HeartbeatReport report = DefaultSaxParser.parse(xml);

		return report;
	}
}
