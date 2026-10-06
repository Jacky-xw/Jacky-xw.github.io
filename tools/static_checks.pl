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
