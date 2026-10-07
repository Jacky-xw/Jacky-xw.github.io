use strict; use warnings;
sub readfile { local $/; open my $f,'<',$_[0] or die $!; return <$f>; }
my $root='app/src/main/java/com/ruru/practice/';
my $service=readfile($root.'feature/meditation/MeditationKeepAliveService.kt');
die 'wrong Handler clock' if $service =~ /postAtTime/;
die 'obsolete restore field' if $service =~ /remaining_seconds/;
die 'missing deadline helper' unless $service =~ /TimerDeadline.restore/ && $service =~ /TimerDeadline.nextCheckDelay/;
for my $screen (qw(meditation/MeditationScreen practice/WalkingScreen practice/RootProtectionScreen practice/PracticeLogScreen practice/EightPreceptsScreen observation/ObservationScreen reflection/ReflectionScreen)) {
 my $text=readfile($root."feature/$screen.kt");
 die "lazy form: $screen" if $text =~ /LazyColumn/;
 die "missing retained form: $screen" unless $text =~ /RetainedColumn\(/;
}
for my $screen (qw(home/HomeScreen learning/LearningScreen)) { die 'missing virtual list' unless readfile($root."feature/$screen.kt") =~ /LazyColumn\(/; }
my @files=split /\n/, `find app/src -name '*.kt'`;
for my $p (@files) {
 my $s=readfile($p);
 # Lightweight lexical balance only. Not a Kotlin compiler/type checker.
 $s =~ s/""".*?"""|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\/\*.*?\*\/|\/\/[^\n]*//sg;
 my @stack;
 my %close=('}'=>'{',')'=>'(',']'=>'[');
 while ($s =~ /([{}()\[\]])/g) {
  my $c=$1;
  if (exists $close{$c}) { die "unbalanced $p" unless @stack && pop(@stack) eq $close{$c}; }
  else { push @stack,$c; }
 }
 die "unclosed $p" if @stack;
}
print "PASS: scroll policy, timer source invariants, delimiter balance (".scalar(@files)." Kotlin files).\n";
my $scheduler=readfile($root.'feature/meditation/MeditationEndScheduler.kt');
my $receiver=readfile($root.'feature/meditation/MeditationEndAlarmReceiver.kt');
die 'missing broadcast alarm' unless $scheduler =~ /PendingIntent.getBroadcast/ && $scheduler =~ /am.setAlarmClock/;
die 'receiver must not finish session before service playback' if $receiver =~ /publishNaturalEndDue/;
die 'missing handoff lock' unless $receiver =~ /TimerAlarmHandoff.acquire/ && $service =~ /TimerAlarmHandoff.release/;
die 'missing alarm token validation' unless $receiver =~ /TimerDeadline.matchesAlarm/ && $service =~ /TimerDeadline.matchesAlarm/;
die 'missing diagnostic export' unless readfile($root.'feature/meditation/MeditationScreen.kt') =~ /TimerDiagnosticsButton\(\)/;
print "PASS: Android 14 alarm wiring source checks (not runtime tests).\n";
die 'synchronous keepalive prepare on service' if $service =~ /player\.prepare\(\)/;
die 'missing media worker / async prepare' unless $service =~ /HandlerThread\("timer-media"\)/ && $service =~ /player.prepareAsync\(\)/;
die 'missing independent diagnostic probe' unless $service =~ /TimerExecutionProbe/;
my $vm=readfile($root.'feature/meditation/MeditationSessionViewModel.kt');
die 'natural completion cancels alarm before service handoff' unless $vm =~ /if \(!naturalEnd\) cancelWallClockEnd\(\)/;
print "PASS: 1.41.2 media/probe/handoff source invariants.\n";
my $diag=readfile($root.'feature/meditation/TimerDiagnostics.kt');
my $probe=readfile($root.'feature/meditation/TimerExecutionProbe.kt');
die 'system query blocks probe' if $probe =~ /TimerDiagnostics.deviceState/;
die 'missing bounded per-session export' unless $diag =~ /DiagnosticStore/ && $diag =~ /ZipOutputStream/ && $diag =~ /pending.incrementAndGet/;
die 'missing new meditation session id' unless $vm =~ /TimerDiagnostics.beginSession/;
die 'missing diagnostic SAF export UI' unless readfile($root.'feature/meditation/TimerDiagnosticsButton.kt') =~ /CreateDocument\("application\/zip"\)/;
print "PASS: 1.41.3 diagnostic storage/export source invariants.\n";
