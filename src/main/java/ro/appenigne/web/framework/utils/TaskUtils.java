package ro.appenigne.web.framework.utils;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import java.util.ArrayList;
import java.util.List;

public class TaskUtils {

	public static void addTaskChannel(String queueParam, List<TaskOptions> tasks) {
		Queue queue = QueueFactory.getQueue(queueParam);

		if (tasks.size() < 100) {
			queue.add(tasks);
		} else {
			List<TaskOptions> ts = new ArrayList<>();
			for (TaskOptions task : tasks) {
				ts.add(task);
				if (ts.size() >= 100) {
					queue.add(ts);
					ts = new ArrayList<>();
				}
			}
			if (ts.size() > 0) {
				queue.add(ts);
			}
		}
	}
}
