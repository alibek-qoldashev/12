package uz.focus.examlock;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Zaxira: har 15 daqiqada tekshiradi va servisni qayta ishga tushiradi. */
public class ScanJob extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Lock.enforce(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
