¤ý
Í£
8
Const
output"dtype"
valuetensor"
dtypetype

NoOp
C
Placeholder
output"dtype"
dtypetype"
shapeshape:
@
ReadVariableOp
resource
value"dtype"
dtypetype
¾
StatefulPartitionedCall
args2Tin
output2Tout"
Tin
list(type)("
Tout
list(type)("	
ffunc"
configstring "
config_protostring "
executor_typestring 

VarHandleOp
resource"
	containerstring "
shared_namestring "
dtypetype"
shapeshape"#
allowed_deviceslist(string)
 "serve*2.3.12v2.3.0-54-gfcc4b966f18ã
d
VariableVarHandleOp*
_output_shapes
: *
dtype0*
shape: *
shared_name
Variable
]
Variable/Read/ReadVariableOpReadVariableOpVariable*
_output_shapes
: *
dtype0
`
adam/tVarHandleOp*
_output_shapes
: *
dtype0	*
shape: *
shared_nameadam/t
Y
adam/t/Read/ReadVariableOpReadVariableOpadam/t*
_output_shapes
: *
dtype0	
d
adam/t_1VarHandleOp*
_output_shapes
: *
dtype0	*
shape: *
shared_name
adam/t_1
]
adam/t_1/Read/ReadVariableOpReadVariableOpadam/t_1*
_output_shapes
: *
dtype0	

adam/m_1/dense/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*&
shared_nameadam/m_1/dense/kernel

)adam/m_1/dense/kernel/Read/ReadVariableOpReadVariableOpadam/m_1/dense/kernel*
_output_shapes
:	*
dtype0

adam/m_1/dense/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*$
shared_nameadam/m_1/dense/bias
x
'adam/m_1/dense/bias/Read/ReadVariableOpReadVariableOpadam/m_1/dense/bias*
_output_shapes	
:*
dtype0

adam/m_1/dense_1/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:
*(
shared_nameadam/m_1/dense_1/kernel

+adam/m_1/dense_1/kernel/Read/ReadVariableOpReadVariableOpadam/m_1/dense_1/kernel* 
_output_shapes
:
*
dtype0

adam/m_1/dense_1/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*&
shared_nameadam/m_1/dense_1/bias
|
)adam/m_1/dense_1/bias/Read/ReadVariableOpReadVariableOpadam/m_1/dense_1/bias*
_output_shapes	
:*
dtype0

adam/m_1/dense_2/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*(
shared_nameadam/m_1/dense_2/kernel

+adam/m_1/dense_2/kernel/Read/ReadVariableOpReadVariableOpadam/m_1/dense_2/kernel*
_output_shapes
:	*
dtype0

adam/m_1/dense_2/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*&
shared_nameadam/m_1/dense_2/bias
{
)adam/m_1/dense_2/bias/Read/ReadVariableOpReadVariableOpadam/m_1/dense_2/bias*
_output_shapes
:*
dtype0

adam/v_1/dense/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*&
shared_nameadam/v_1/dense/kernel

)adam/v_1/dense/kernel/Read/ReadVariableOpReadVariableOpadam/v_1/dense/kernel*
_output_shapes
:	*
dtype0

adam/v_1/dense/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*$
shared_nameadam/v_1/dense/bias
x
'adam/v_1/dense/bias/Read/ReadVariableOpReadVariableOpadam/v_1/dense/bias*
_output_shapes	
:*
dtype0

adam/v_1/dense_1/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:
*(
shared_nameadam/v_1/dense_1/kernel

+adam/v_1/dense_1/kernel/Read/ReadVariableOpReadVariableOpadam/v_1/dense_1/kernel* 
_output_shapes
:
*
dtype0

adam/v_1/dense_1/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*&
shared_nameadam/v_1/dense_1/bias
|
)adam/v_1/dense_1/bias/Read/ReadVariableOpReadVariableOpadam/v_1/dense_1/bias*
_output_shapes	
:*
dtype0

adam/v_1/dense_2/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*(
shared_nameadam/v_1/dense_2/kernel

+adam/v_1/dense_2/kernel/Read/ReadVariableOpReadVariableOpadam/v_1/dense_2/kernel*
_output_shapes
:	*
dtype0

adam/v_1/dense_2/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*&
shared_nameadam/v_1/dense_2/bias
{
)adam/v_1/dense_2/bias/Read/ReadVariableOpReadVariableOpadam/v_1/dense_2/bias*
_output_shapes
:*
dtype0

adam/m/dense_3/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*&
shared_nameadam/m/dense_3/kernel

)adam/m/dense_3/kernel/Read/ReadVariableOpReadVariableOpadam/m/dense_3/kernel*
_output_shapes
:	*
dtype0

adam/m/dense_3/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*$
shared_nameadam/m/dense_3/bias
x
'adam/m/dense_3/bias/Read/ReadVariableOpReadVariableOpadam/m/dense_3/bias*
_output_shapes	
:*
dtype0

adam/m/dense_4/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:
*&
shared_nameadam/m/dense_4/kernel

)adam/m/dense_4/kernel/Read/ReadVariableOpReadVariableOpadam/m/dense_4/kernel* 
_output_shapes
:
*
dtype0

adam/m/dense_4/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*$
shared_nameadam/m/dense_4/bias
x
'adam/m/dense_4/bias/Read/ReadVariableOpReadVariableOpadam/m/dense_4/bias*
_output_shapes	
:*
dtype0

adam/m/dense_5/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*&
shared_nameadam/m/dense_5/kernel

)adam/m/dense_5/kernel/Read/ReadVariableOpReadVariableOpadam/m/dense_5/kernel*
_output_shapes
:	*
dtype0
~
adam/m/dense_5/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*$
shared_nameadam/m/dense_5/bias
w
'adam/m/dense_5/bias/Read/ReadVariableOpReadVariableOpadam/m/dense_5/bias*
_output_shapes
:*
dtype0

adam/v/dense_3/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*&
shared_nameadam/v/dense_3/kernel

)adam/v/dense_3/kernel/Read/ReadVariableOpReadVariableOpadam/v/dense_3/kernel*
_output_shapes
:	*
dtype0

adam/v/dense_3/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*$
shared_nameadam/v/dense_3/bias
x
'adam/v/dense_3/bias/Read/ReadVariableOpReadVariableOpadam/v/dense_3/bias*
_output_shapes	
:*
dtype0

adam/v/dense_4/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:
*&
shared_nameadam/v/dense_4/kernel

)adam/v/dense_4/kernel/Read/ReadVariableOpReadVariableOpadam/v/dense_4/kernel* 
_output_shapes
:
*
dtype0

adam/v/dense_4/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*$
shared_nameadam/v/dense_4/bias
x
'adam/v/dense_4/bias/Read/ReadVariableOpReadVariableOpadam/v/dense_4/bias*
_output_shapes	
:*
dtype0

adam/v/dense_5/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*&
shared_nameadam/v/dense_5/kernel

)adam/v/dense_5/kernel/Read/ReadVariableOpReadVariableOpadam/v/dense_5/kernel*
_output_shapes
:	*
dtype0
~
adam/v/dense_5/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*$
shared_nameadam/v/dense_5/bias
w
'adam/v/dense_5/bias/Read/ReadVariableOpReadVariableOpadam/v/dense_5/bias*
_output_shapes
:*
dtype0
u
dense/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*
shared_namedense/kernel
n
 dense/kernel/Read/ReadVariableOpReadVariableOpdense/kernel*
_output_shapes
:	*
dtype0
m

dense/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*
shared_name
dense/bias
f
dense/bias/Read/ReadVariableOpReadVariableOp
dense/bias*
_output_shapes	
:*
dtype0
z
dense_1/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:
*
shared_namedense_1/kernel
s
"dense_1/kernel/Read/ReadVariableOpReadVariableOpdense_1/kernel* 
_output_shapes
:
*
dtype0
q
dense_1/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*
shared_namedense_1/bias
j
 dense_1/bias/Read/ReadVariableOpReadVariableOpdense_1/bias*
_output_shapes	
:*
dtype0
y
dense_2/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*
shared_namedense_2/kernel
r
"dense_2/kernel/Read/ReadVariableOpReadVariableOpdense_2/kernel*
_output_shapes
:	*
dtype0
p
dense_2/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*
shared_namedense_2/bias
i
 dense_2/bias/Read/ReadVariableOpReadVariableOpdense_2/bias*
_output_shapes
:*
dtype0
y
dense_3/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*
shared_namedense_3/kernel
r
"dense_3/kernel/Read/ReadVariableOpReadVariableOpdense_3/kernel*
_output_shapes
:	*
dtype0
q
dense_3/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*
shared_namedense_3/bias
j
 dense_3/bias/Read/ReadVariableOpReadVariableOpdense_3/bias*
_output_shapes	
:*
dtype0
z
dense_4/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:
*
shared_namedense_4/kernel
s
"dense_4/kernel/Read/ReadVariableOpReadVariableOpdense_4/kernel* 
_output_shapes
:
*
dtype0
q
dense_4/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*
shared_namedense_4/bias
j
 dense_4/bias/Read/ReadVariableOpReadVariableOpdense_4/bias*
_output_shapes	
:*
dtype0
y
dense_5/kernelVarHandleOp*
_output_shapes
: *
dtype0*
shape:	*
shared_namedense_5/kernel
r
"dense_5/kernel/Read/ReadVariableOpReadVariableOpdense_5/kernel*
_output_shapes
:	*
dtype0
p
dense_5/biasVarHandleOp*
_output_shapes
: *
dtype0*
shape:*
shared_namedense_5/bias
i
 dense_5/bias/Read/ReadVariableOpReadVariableOpdense_5/bias*
_output_shapes
:*
dtype0

NoOpNoOp
;
ConstConst"/device:CPU:0*
_output_shapes
: *
dtype0*Ç:
value½:Bº: B³:

critic_optimizer
actor_optimizer
online_critic
target_critic
online_actor
target_actor
buffer_size

signatures

	step

m
v

step
m
v

critic_layers

critic_layers

actor_layers

actor_layers
DB
VARIABLE_VALUEVariable&buffer_size/.ATTRIBUTES/VARIABLE_VALUE
 
LJ
VARIABLE_VALUEadam/t0critic_optimizer/step/.ATTRIBUTES/VARIABLE_VALUE
*
0
1
2
3
4
5
*
0
1
2
3
4
5
MK
VARIABLE_VALUEadam/t_1/actor_optimizer/step/.ATTRIBUTES/VARIABLE_VALUE
*
0
1
2
 3
!4
"5
*
#0
$1
%2
&3
'4
(5
Ç
)layer_with_weights-0
)layer-0
*layer_with_weights-1
*layer-1
+layer_with_weights-2
+layer-2
,trainable_variables
-	variables
.regularization_losses
/	keras_api
Ç
0layer_with_weights-0
0layer-0
1layer_with_weights-1
1layer-1
2layer_with_weights-2
2layer-2
3trainable_variables
4	variables
5regularization_losses
6	keras_api
ZX
VARIABLE_VALUEadam/m_1/dense/kernel/critic_optimizer/m/0/.ATTRIBUTES/VARIABLE_VALUE
XV
VARIABLE_VALUEadam/m_1/dense/bias/critic_optimizer/m/1/.ATTRIBUTES/VARIABLE_VALUE
\Z
VARIABLE_VALUEadam/m_1/dense_1/kernel/critic_optimizer/m/2/.ATTRIBUTES/VARIABLE_VALUE
ZX
VARIABLE_VALUEadam/m_1/dense_1/bias/critic_optimizer/m/3/.ATTRIBUTES/VARIABLE_VALUE
\Z
VARIABLE_VALUEadam/m_1/dense_2/kernel/critic_optimizer/m/4/.ATTRIBUTES/VARIABLE_VALUE
ZX
VARIABLE_VALUEadam/m_1/dense_2/bias/critic_optimizer/m/5/.ATTRIBUTES/VARIABLE_VALUE
ZX
VARIABLE_VALUEadam/v_1/dense/kernel/critic_optimizer/v/0/.ATTRIBUTES/VARIABLE_VALUE
XV
VARIABLE_VALUEadam/v_1/dense/bias/critic_optimizer/v/1/.ATTRIBUTES/VARIABLE_VALUE
\Z
VARIABLE_VALUEadam/v_1/dense_1/kernel/critic_optimizer/v/2/.ATTRIBUTES/VARIABLE_VALUE
ZX
VARIABLE_VALUEadam/v_1/dense_1/bias/critic_optimizer/v/3/.ATTRIBUTES/VARIABLE_VALUE
\Z
VARIABLE_VALUEadam/v_1/dense_2/kernel/critic_optimizer/v/4/.ATTRIBUTES/VARIABLE_VALUE
ZX
VARIABLE_VALUEadam/v_1/dense_2/bias/critic_optimizer/v/5/.ATTRIBUTES/VARIABLE_VALUE
YW
VARIABLE_VALUEadam/m/dense_3/kernel.actor_optimizer/m/0/.ATTRIBUTES/VARIABLE_VALUE
WU
VARIABLE_VALUEadam/m/dense_3/bias.actor_optimizer/m/1/.ATTRIBUTES/VARIABLE_VALUE
YW
VARIABLE_VALUEadam/m/dense_4/kernel.actor_optimizer/m/2/.ATTRIBUTES/VARIABLE_VALUE
WU
VARIABLE_VALUEadam/m/dense_4/bias.actor_optimizer/m/3/.ATTRIBUTES/VARIABLE_VALUE
YW
VARIABLE_VALUEadam/m/dense_5/kernel.actor_optimizer/m/4/.ATTRIBUTES/VARIABLE_VALUE
WU
VARIABLE_VALUEadam/m/dense_5/bias.actor_optimizer/m/5/.ATTRIBUTES/VARIABLE_VALUE
YW
VARIABLE_VALUEadam/v/dense_3/kernel.actor_optimizer/v/0/.ATTRIBUTES/VARIABLE_VALUE
WU
VARIABLE_VALUEadam/v/dense_3/bias.actor_optimizer/v/1/.ATTRIBUTES/VARIABLE_VALUE
YW
VARIABLE_VALUEadam/v/dense_4/kernel.actor_optimizer/v/2/.ATTRIBUTES/VARIABLE_VALUE
WU
VARIABLE_VALUEadam/v/dense_4/bias.actor_optimizer/v/3/.ATTRIBUTES/VARIABLE_VALUE
YW
VARIABLE_VALUEadam/v/dense_5/kernel.actor_optimizer/v/4/.ATTRIBUTES/VARIABLE_VALUE
WU
VARIABLE_VALUEadam/v/dense_5/bias.actor_optimizer/v/5/.ATTRIBUTES/VARIABLE_VALUE

7_inbound_nodes

8kernel
9bias
:_outbound_nodes
;trainable_variables
<	variables
=regularization_losses
>	keras_api

?_inbound_nodes

@kernel
Abias
B_outbound_nodes
Ctrainable_variables
D	variables
Eregularization_losses
F	keras_api
|
G_inbound_nodes

Hkernel
Ibias
Jtrainable_variables
K	variables
Lregularization_losses
M	keras_api
*
80
91
@2
A3
H4
I5
*
80
91
@2
A3
H4
I5
 
­
Nlayer_metrics
Onon_trainable_variables
,trainable_variables

Players
-	variables
.regularization_losses
Qmetrics
Rlayer_regularization_losses

S_inbound_nodes

Tkernel
Ubias
V_outbound_nodes
Wtrainable_variables
X	variables
Yregularization_losses
Z	keras_api

[_inbound_nodes

\kernel
]bias
^_outbound_nodes
_trainable_variables
`	variables
aregularization_losses
b	keras_api
|
c_inbound_nodes

dkernel
ebias
ftrainable_variables
g	variables
hregularization_losses
i	keras_api
*
T0
U1
\2
]3
d4
e5
*
T0
U1
\2
]3
d4
e5
 
­
jlayer_metrics
knon_trainable_variables
3trainable_variables

llayers
4	variables
5regularization_losses
mmetrics
nlayer_regularization_losses
 
tr
VARIABLE_VALUEdense/kernelRonline_critic/critic_layers/layer_with_weights-0/kernel/.ATTRIBUTES/VARIABLE_VALUE
pn
VARIABLE_VALUE
dense/biasPonline_critic/critic_layers/layer_with_weights-0/bias/.ATTRIBUTES/VARIABLE_VALUE
 

80
91

80
91
 
­
olayer_metrics
pnon_trainable_variables
;trainable_variables

qlayers
<	variables
=regularization_losses
rmetrics
slayer_regularization_losses
 
vt
VARIABLE_VALUEdense_1/kernelRonline_critic/critic_layers/layer_with_weights-1/kernel/.ATTRIBUTES/VARIABLE_VALUE
rp
VARIABLE_VALUEdense_1/biasPonline_critic/critic_layers/layer_with_weights-1/bias/.ATTRIBUTES/VARIABLE_VALUE
 

@0
A1

@0
A1
 
­
tlayer_metrics
unon_trainable_variables
Ctrainable_variables

vlayers
D	variables
Eregularization_losses
wmetrics
xlayer_regularization_losses
 
vt
VARIABLE_VALUEdense_2/kernelRonline_critic/critic_layers/layer_with_weights-2/kernel/.ATTRIBUTES/VARIABLE_VALUE
rp
VARIABLE_VALUEdense_2/biasPonline_critic/critic_layers/layer_with_weights-2/bias/.ATTRIBUTES/VARIABLE_VALUE

H0
I1

H0
I1
 
­
ylayer_metrics
znon_trainable_variables
Jtrainable_variables

{layers
K	variables
Lregularization_losses
|metrics
}layer_regularization_losses
 
 

)0
*1
+2
 
 
 
tr
VARIABLE_VALUEdense_3/kernelPonline_actor/actor_layers/layer_with_weights-0/kernel/.ATTRIBUTES/VARIABLE_VALUE
pn
VARIABLE_VALUEdense_3/biasNonline_actor/actor_layers/layer_with_weights-0/bias/.ATTRIBUTES/VARIABLE_VALUE
 

T0
U1

T0
U1
 
°
~layer_metrics
non_trainable_variables
Wtrainable_variables
layers
X	variables
Yregularization_losses
metrics
 layer_regularization_losses
 
tr
VARIABLE_VALUEdense_4/kernelPonline_actor/actor_layers/layer_with_weights-1/kernel/.ATTRIBUTES/VARIABLE_VALUE
pn
VARIABLE_VALUEdense_4/biasNonline_actor/actor_layers/layer_with_weights-1/bias/.ATTRIBUTES/VARIABLE_VALUE
 

\0
]1

\0
]1
 
²
layer_metrics
non_trainable_variables
_trainable_variables
layers
`	variables
aregularization_losses
metrics
 layer_regularization_losses
 
tr
VARIABLE_VALUEdense_5/kernelPonline_actor/actor_layers/layer_with_weights-2/kernel/.ATTRIBUTES/VARIABLE_VALUE
pn
VARIABLE_VALUEdense_5/biasNonline_actor/actor_layers/layer_with_weights-2/bias/.ATTRIBUTES/VARIABLE_VALUE

d0
e1

d0
e1
 
²
layer_metrics
non_trainable_variables
ftrainable_variables
layers
g	variables
hregularization_losses
metrics
 layer_regularization_losses
 
 

00
11
22
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
X
serving_default_askSizePlaceholder*
_output_shapes
: *
dtype0*
shape: 
t
serving_default_askVectorPlaceholder*#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*
dtype0*
shape:ÿÿÿÿÿÿÿÿÿ
X
serving_default_bidSizePlaceholder*
_output_shapes
: *
dtype0*
shape: 
t
serving_default_bidVectorPlaceholder*#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*
dtype0*
shape:ÿÿÿÿÿÿÿÿÿ
a
 serving_default_contractHoldingsPlaceholder*
_output_shapes
: *
dtype0*
shape: 
i
(serving_default_finalFundamentalEstimatePlaceholder*
_output_shapes
: *
dtype0*
shape: 
X
serving_default_latencyPlaceholder*
_output_shapes
: *
dtype0	*
shape: 
_
serving_default_marketHoldingsPlaceholder*
_output_shapes
: *
dtype0*
shape: 
`
serving_default_numTransactionsPlaceholder*
_output_shapes
: *
dtype0*
shape: 
^
serving_default_omegaRatioAskPlaceholder*
_output_shapes
: *
dtype0*
shape: 
^
serving_default_omegaRatioBidPlaceholder*
_output_shapes
: *
dtype0*
shape: 
[
serving_default_privateAskPlaceholder*
_output_shapes
: *
dtype0*
shape: 
[
serving_default_privateBidPlaceholder*
_output_shapes
: *
dtype0*
shape: 
U
serving_default_sidePlaceholder*
_output_shapes
: *
dtype0*
shape: 
W
serving_default_spreadPlaceholder*
_output_shapes
: *
dtype0*
shape: 
c
"serving_default_timeSinceLastTradePlaceholder*
_output_shapes
: *
dtype0	*
shape: 
[
serving_default_timeTilEndPlaceholder*
_output_shapes
: *
dtype0	*
shape: 
}
"serving_default_transactionHistoryPlaceholder*#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*
dtype0*
shape:ÿÿÿÿÿÿÿÿÿ
¯
StatefulPartitionedCallStatefulPartitionedCallserving_default_askSizeserving_default_askVectorserving_default_bidSizeserving_default_bidVector serving_default_contractHoldings(serving_default_finalFundamentalEstimateserving_default_latencyserving_default_marketHoldingsserving_default_numTransactionsserving_default_omegaRatioAskserving_default_omegaRatioBidserving_default_privateAskserving_default_privateBidserving_default_sideserving_default_spread"serving_default_timeSinceLastTradeserving_default_timeTilEnd"serving_default_transactionHistoryVariabledense_3/kerneldense_3/biasdense_4/kerneldense_4/biasdense_5/kerneldense_5/bias*$
Tin
2			*
Tout	
2*
_collective_manager_ids
 *"
_output_shapes
:: : :: *(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 *,
f'R%
#__inference_signature_wrapper_12756
O
saver_filenamePlaceholder*
_output_shapes
: *
dtype0*
shape: 
¹
StatefulPartitionedCall_1StatefulPartitionedCallsaver_filenameVariable/Read/ReadVariableOpadam/t/Read/ReadVariableOpadam/t_1/Read/ReadVariableOp)adam/m_1/dense/kernel/Read/ReadVariableOp'adam/m_1/dense/bias/Read/ReadVariableOp+adam/m_1/dense_1/kernel/Read/ReadVariableOp)adam/m_1/dense_1/bias/Read/ReadVariableOp+adam/m_1/dense_2/kernel/Read/ReadVariableOp)adam/m_1/dense_2/bias/Read/ReadVariableOp)adam/v_1/dense/kernel/Read/ReadVariableOp'adam/v_1/dense/bias/Read/ReadVariableOp+adam/v_1/dense_1/kernel/Read/ReadVariableOp)adam/v_1/dense_1/bias/Read/ReadVariableOp+adam/v_1/dense_2/kernel/Read/ReadVariableOp)adam/v_1/dense_2/bias/Read/ReadVariableOp)adam/m/dense_3/kernel/Read/ReadVariableOp'adam/m/dense_3/bias/Read/ReadVariableOp)adam/m/dense_4/kernel/Read/ReadVariableOp'adam/m/dense_4/bias/Read/ReadVariableOp)adam/m/dense_5/kernel/Read/ReadVariableOp'adam/m/dense_5/bias/Read/ReadVariableOp)adam/v/dense_3/kernel/Read/ReadVariableOp'adam/v/dense_3/bias/Read/ReadVariableOp)adam/v/dense_4/kernel/Read/ReadVariableOp'adam/v/dense_4/bias/Read/ReadVariableOp)adam/v/dense_5/kernel/Read/ReadVariableOp'adam/v/dense_5/bias/Read/ReadVariableOp dense/kernel/Read/ReadVariableOpdense/bias/Read/ReadVariableOp"dense_1/kernel/Read/ReadVariableOp dense_1/bias/Read/ReadVariableOp"dense_2/kernel/Read/ReadVariableOp dense_2/bias/Read/ReadVariableOp"dense_3/kernel/Read/ReadVariableOp dense_3/bias/Read/ReadVariableOp"dense_4/kernel/Read/ReadVariableOp dense_4/bias/Read/ReadVariableOp"dense_5/kernel/Read/ReadVariableOp dense_5/bias/Read/ReadVariableOpConst*4
Tin-
+2)		*
Tout
2*
_collective_manager_ids
 *
_output_shapes
: * 
_read_only_resource_inputs
 *-
config_proto

CPU

GPU 2J 8 *'
f"R 
__inference__traced_save_13748
¨
StatefulPartitionedCall_2StatefulPartitionedCallsaver_filenameVariableadam/tadam/t_1adam/m_1/dense/kerneladam/m_1/dense/biasadam/m_1/dense_1/kerneladam/m_1/dense_1/biasadam/m_1/dense_2/kerneladam/m_1/dense_2/biasadam/v_1/dense/kerneladam/v_1/dense/biasadam/v_1/dense_1/kerneladam/v_1/dense_1/biasadam/v_1/dense_2/kerneladam/v_1/dense_2/biasadam/m/dense_3/kerneladam/m/dense_3/biasadam/m/dense_4/kerneladam/m/dense_4/biasadam/m/dense_5/kerneladam/m/dense_5/biasadam/v/dense_3/kerneladam/v/dense_3/biasadam/v/dense_4/kerneladam/v/dense_4/biasadam/v/dense_5/kerneladam/v/dense_5/biasdense/kernel
dense/biasdense_1/kerneldense_1/biasdense_2/kerneldense_2/biasdense_3/kerneldense_3/biasdense_4/kerneldense_4/biasdense_5/kerneldense_5/bias*3
Tin,
*2(*
Tout
2*
_collective_manager_ids
 *
_output_shapes
: * 
_read_only_resource_inputs
 *-
config_proto

CPU

GPU 2J 8 **
f%R#
!__inference__traced_restore_13875Ò¯
°
ª
B__inference_dense_1_layer_call_and_return_conditional_losses_12823

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
MatMul/ReadVariableOpt
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddY
ReluReluBiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Relug
IdentityIdentityRelu:activations:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
Ú
|
'__inference_dense_5_layer_call_fn_13587

inputs
unknown
	unknown_0
identity¢StatefulPartitionedCallò
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0*
Tin
2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_5_layer_call_and_return_conditional_losses_130272
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ::22
StatefulPartitionedCallStatefulPartitionedCall:P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
 
ª
B__inference_dense_5_layer_call_and_return_conditional_losses_13578

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource*
_output_shapes
:	*
dtype02
MatMul/ReadVariableOps
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddX
TanhTanhBiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Tanh\
IdentityIdentityTanh:y:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs

ñ
G__inference_sequential_1_layer_call_and_return_conditional_losses_13409

inputs*
&dense_3_matmul_readvariableop_resource+
'dense_3_biasadd_readvariableop_resource*
&dense_4_matmul_readvariableop_resource+
'dense_4_biasadd_readvariableop_resource*
&dense_5_matmul_readvariableop_resource+
'dense_5_biasadd_readvariableop_resource
identity¦
dense_3/MatMul/ReadVariableOpReadVariableOp&dense_3_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_3/MatMul/ReadVariableOp
dense_3/MatMulMatMulinputs%dense_3/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/MatMul¥
dense_3/BiasAdd/ReadVariableOpReadVariableOp'dense_3_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_3/BiasAdd/ReadVariableOp¢
dense_3/BiasAddBiasAdddense_3/MatMul:product:0&dense_3/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/BiasAddq
dense_3/ReluReludense_3/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/Relu§
dense_4/MatMul/ReadVariableOpReadVariableOp&dense_4_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
dense_4/MatMul/ReadVariableOp 
dense_4/MatMulMatMuldense_3/Relu:activations:0%dense_4/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/MatMul¥
dense_4/BiasAdd/ReadVariableOpReadVariableOp'dense_4_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_4/BiasAdd/ReadVariableOp¢
dense_4/BiasAddBiasAdddense_4/MatMul:product:0&dense_4/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/BiasAddq
dense_4/ReluReludense_4/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/Relu¦
dense_5/MatMul/ReadVariableOpReadVariableOp&dense_5_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_5/MatMul/ReadVariableOp
dense_5/MatMulMatMuldense_4/Relu:activations:0%dense_5/MatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/MatMul¤
dense_5/BiasAdd/ReadVariableOpReadVariableOp'dense_5_biasadd_readvariableop_resource*
_output_shapes
:*
dtype02 
dense_5/BiasAdd/ReadVariableOp¡
dense_5/BiasAddBiasAdddense_5/MatMul:product:0&dense_5/BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/BiasAddp
dense_5/TanhTanhdense_5/BiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/Tanhd
IdentityIdentitydense_5/Tanh:y:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ:::::::O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
ß
»
*__inference_sequential_layer_call_fn_13201

inputs
unknown
	unknown_0
	unknown_1
	unknown_2
	unknown_3
	unknown_4
identity¢StatefulPartitionedCall©
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0	unknown_1	unknown_2	unknown_3	unknown_4*
Tin
	2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 *N
fIRG
E__inference_sequential_layer_call_and_return_conditional_losses_129072
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::22
StatefulPartitionedCallStatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
Ï
^
cond_1_true_479
cond_1_identity_privatebid
cond_1_placeholder
cond_1_identityk
cond_1/IdentityIdentitycond_1_identity_privatebid*
T0*
_output_shapes
: 2
cond_1/Identity"+
cond_1_identitycond_1/Identity:output:0*
_input_shapes
: : : 

_output_shapes
: :

_output_shapes
: 
Ú
|
'__inference_dense_3_layer_call_fn_13547

inputs
unknown
	unknown_0
identity¢StatefulPartitionedCalló
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_3_layer_call_and_return_conditional_losses_129732
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*.
_input_shapes
:ÿÿÿÿÿÿÿÿÿ::22
StatefulPartitionedCallStatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs

¾
G__inference_sequential_1_layer_call_and_return_conditional_losses_13085

inputs
dense_3_13069
dense_3_13071
dense_4_13074
dense_4_13076
dense_5_13079
dense_5_13081
identity¢dense_3/StatefulPartitionedCall¢dense_4/StatefulPartitionedCall¢dense_5/StatefulPartitionedCall
dense_3/StatefulPartitionedCallStatefulPartitionedCallinputsdense_3_13069dense_3_13071*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_3_layer_call_and_return_conditional_losses_129732!
dense_3/StatefulPartitionedCall¯
dense_4/StatefulPartitionedCallStatefulPartitionedCall(dense_3/StatefulPartitionedCall:output:0dense_4_13074dense_4_13076*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_4_layer_call_and_return_conditional_losses_130002!
dense_4/StatefulPartitionedCall®
dense_5/StatefulPartitionedCallStatefulPartitionedCall(dense_4/StatefulPartitionedCall:output:0dense_5_13079dense_5_13081*
Tin
2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_5_layer_call_and_return_conditional_losses_130272!
dense_5/StatefulPartitionedCallâ
IdentityIdentity(dense_5/StatefulPartitionedCall:output:0 ^dense_3/StatefulPartitionedCall ^dense_4/StatefulPartitionedCall ^dense_5/StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::2B
dense_3/StatefulPartitionedCalldense_3/StatefulPartitionedCall2B
dense_4/StatefulPartitionedCalldense_4/StatefulPartitionedCall2B
dense_5/StatefulPartitionedCalldense_5/StatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs

¾
G__inference_sequential_1_layer_call_and_return_conditional_losses_13121

inputs
dense_3_13105
dense_3_13107
dense_4_13110
dense_4_13112
dense_5_13115
dense_5_13117
identity¢dense_3/StatefulPartitionedCall¢dense_4/StatefulPartitionedCall¢dense_5/StatefulPartitionedCall
dense_3/StatefulPartitionedCallStatefulPartitionedCallinputsdense_3_13105dense_3_13107*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_3_layer_call_and_return_conditional_losses_129732!
dense_3/StatefulPartitionedCall¯
dense_4/StatefulPartitionedCallStatefulPartitionedCall(dense_3/StatefulPartitionedCall:output:0dense_4_13110dense_4_13112*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_4_layer_call_and_return_conditional_losses_130002!
dense_4/StatefulPartitionedCall®
dense_5/StatefulPartitionedCallStatefulPartitionedCall(dense_4/StatefulPartitionedCall:output:0dense_5_13115dense_5_13117*
Tin
2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_5_layer_call_and_return_conditional_losses_130272!
dense_5/StatefulPartitionedCallâ
IdentityIdentity(dense_5/StatefulPartitionedCall:output:0 ^dense_3/StatefulPartitionedCall ^dense_4/StatefulPartitionedCall ^dense_5/StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::2B
dense_3/StatefulPartitionedCalldense_3/StatefulPartitionedCall2B
dense_4/StatefulPartitionedCalldense_4/StatefulPartitionedCall2B
dense_5/StatefulPartitionedCalldense_5/StatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
Ù0
Ô
__inference__policy_513
finalfundamentalestimate

privatebid

privateask
omegaratiobid
omegaratioask
side
bidsize
asksize

spread
marketholdings
contractholdings
numtransactions

timetilend	
latency	
timesincelasttrade	
	bidvector
	askvector
transactionhistory
readvariableop_resource
cond_input_15
cond_input_16
cond_input_17
cond_input_18
cond_input_19
cond_input_20
identity

identity_1

identity_2

identity_3

identity_4¢AssignAddVariableOp¢condp
ReadVariableOpReadVariableOpreadvariableop_resource*
_output_shapes
: *
dtype02
ReadVariableOpS
Less/yConst*
_output_shapes
: *
dtype0*
value
B :2
Less/y^
LessLessReadVariableOp:value:0Less/y:output:0*
T0*
_output_shapes
: 2
LessÁ
condIfLess:z:0finalfundamentalestimate
privatebid
privateaskomegaratiobidomegaratioasksidebidsizeasksizespreadmarketholdings
timetilendtimesincelasttrade	bidvector	askvectortransactionhistorycond_input_15cond_input_16cond_input_17cond_input_18cond_input_19cond_input_20*
Tcond0
* 
Tin
2		*
Tout
2*
_lower_using_switch_merge(*
_output_shapes
:*(
_read_only_resource_inputs

*!
else_branchR
cond_false_343*
output_shapes
:* 
then_branchR
cond_true_3422
cond\
cond/IdentityIdentitycond:output:0*
T0*
_output_shapes
:2
cond/IdentityT
Equal/yConst*
_output_shapes
: *
dtype0*
value	B :2	
Equal/yp
EqualEqualsideEqual/y:output:0*
T0*
_output_shapes
: *
incompatible_shape_error( 2
Equal¨
cond_1StatelessIf	Equal:z:0
privatebid
privateask*
Tcond0
*
Tin
2*
Tout
2*
_lower_using_switch_merge(*
_output_shapes
: * 
_read_only_resource_inputs
 *#
else_branchR
cond_1_false_480*
output_shapes
: *"
then_branchR
cond_1_true_4792
cond_1`
cond_1/IdentityIdentitycond_1:output:0*
T0*
_output_shapes
: 2
cond_1/Identityw
clip_by_value/Minimum/yConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
clip_by_value/Minimum/y
clip_by_value/MinimumMinimumcond/Identity:output:0 clip_by_value/Minimum/y:output:0*
T0*
_output_shapes
:2
clip_by_value/Minimumg
clip_by_value/yConst*
_output_shapes
: *
dtype0*
valueB
 *    2
clip_by_value/y
clip_by_valueMaximumclip_by_value/Minimum:z:0clip_by_value/y:output:0*
T0*
_output_shapes
:2
clip_by_value^
CastCastfinalfundamentalestimate*

DstT0*

SrcT0*
_output_shapes
: 2
Castb
Cast_1Castcond_1/Identity:output:0*

DstT0*

SrcT0*
_output_shapes
: 2
Cast_1H
AddAddCast:y:0
Cast_1:y:0*
T0*
_output_shapes
: 2
AddN
Cast_2Castside*

DstT0*

SrcT0*
_output_shapes
: 2
Cast_2S
MulMul
Cast_2:y:0clip_by_value:z:0*
T0*
_output_shapes
:2
MulW
mul_1/yConst*
_output_shapes
: *
dtype0*
valueB
 * @D2	
mul_1/yS
mul_1MulMul:z:0mul_1/y:output:0*
T0*
_output_shapes
:2
mul_1H
SubSubAdd:z:0	mul_1:z:0*
T0*
_output_shapes
:2
SubN
sizeConst*
_output_shapes
: *
dtype0*
value	B :2
sizeP
ConstConst*
_output_shapes
: *
dtype0*
value	B :2
Const
AssignAddVariableOpAssignAddVariableOpreadvariableop_resourceConst:output:0^ReadVariableOp*
_output_shapes
 *
dtype02
AssignAddVariableOpS
Cast_3CastSub:z:0*

DstT0*

SrcT0*
_output_shapes
:2
Cast_3
Cast_4/ReadVariableOpReadVariableOpreadvariableop_resource^AssignAddVariableOp*
_output_shapes
: *
dtype02
Cast_4/ReadVariableOpg
Cast_4CastCast_4/ReadVariableOp:value:0*

DstT0*

SrcT0*
_output_shapes
: 2
Cast_4l
IdentityIdentity
Cast_3:y:0^AssignAddVariableOp^cond*
T0*
_output_shapes
:2

Identityh

Identity_1Identityside^AssignAddVariableOp^cond*
T0*
_output_shapes
: 2

Identity_1q

Identity_2Identitysize:output:0^AssignAddVariableOp^cond*
T0*
_output_shapes
: 2

Identity_2w

Identity_3Identityclip_by_value:z:0^AssignAddVariableOp^cond*
T0*
_output_shapes
:2

Identity_3n

Identity_4Identity
Cast_4:y:0^AssignAddVariableOp^cond*
T0*
_output_shapes
: 2

Identity_4"
identityIdentity:output:0"!

identity_1Identity_1:output:0"!

identity_2Identity_2:output:0"!

identity_3Identity_3:output:0"!

identity_4Identity_4:output:0*z
_input_shapesi
g: : : : : : : : : : : : : : : :ÿÿÿÿÿÿÿÿÿ:ÿÿÿÿÿÿÿÿÿ:ÿÿÿÿÿÿÿÿÿ:::::::2*
AssignAddVariableOpAssignAddVariableOp2
condcond:P L

_output_shapes
: 
2
_user_specified_namefinalFundamentalEstimate:B>

_output_shapes
: 
$
_user_specified_name
privateBid:B>

_output_shapes
: 
$
_user_specified_name
privateAsk:EA

_output_shapes
: 
'
_user_specified_nameomegaRatioBid:EA

_output_shapes
: 
'
_user_specified_nameomegaRatioAsk:<8

_output_shapes
: 

_user_specified_nameside:?;

_output_shapes
: 
!
_user_specified_name	bidSize:?;

_output_shapes
: 
!
_user_specified_name	askSize:>:

_output_shapes
: 
 
_user_specified_namespread:F	B

_output_shapes
: 
(
_user_specified_namemarketHoldings:H
D

_output_shapes
: 
*
_user_specified_namecontractHoldings:GC

_output_shapes
: 
)
_user_specified_namenumTransactions:B>

_output_shapes
: 
$
_user_specified_name
timeTilEnd:?;

_output_shapes
: 
!
_user_specified_name	latency:JF

_output_shapes
: 
,
_user_specified_nametimeSinceLastTrade:NJ
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
#
_user_specified_name	bidVector:NJ
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
#
_user_specified_name	askVector:WS
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
,
_user_specified_nametransactionHistory

ð
E__inference_sequential_layer_call_and_return_conditional_losses_13266
dense_input(
$dense_matmul_readvariableop_resource)
%dense_biasadd_readvariableop_resource*
&dense_1_matmul_readvariableop_resource+
'dense_1_biasadd_readvariableop_resource*
&dense_2_matmul_readvariableop_resource+
'dense_2_biasadd_readvariableop_resource
identity 
dense/MatMul/ReadVariableOpReadVariableOp$dense_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense/MatMul/ReadVariableOp
dense/MatMulMatMuldense_input#dense/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense/MatMul
dense/BiasAdd/ReadVariableOpReadVariableOp%dense_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
dense/BiasAdd/ReadVariableOp
dense/BiasAddBiasAdddense/MatMul:product:0$dense/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense/BiasAddk

dense/ReluReludense/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

dense/Relu§
dense_1/MatMul/ReadVariableOpReadVariableOp&dense_1_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
dense_1/MatMul/ReadVariableOp
dense_1/MatMulMatMuldense/Relu:activations:0%dense_1/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/MatMul¥
dense_1/BiasAdd/ReadVariableOpReadVariableOp'dense_1_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_1/BiasAdd/ReadVariableOp¢
dense_1/BiasAddBiasAdddense_1/MatMul:product:0&dense_1/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/BiasAddq
dense_1/ReluReludense_1/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/Relu¦
dense_2/MatMul/ReadVariableOpReadVariableOp&dense_2_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_2/MatMul/ReadVariableOp
dense_2/MatMulMatMuldense_1/Relu:activations:0%dense_2/MatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_2/MatMul¤
dense_2/BiasAdd/ReadVariableOpReadVariableOp'dense_2_biasadd_readvariableop_resource*
_output_shapes
:*
dtype02 
dense_2/BiasAdd/ReadVariableOp¡
dense_2/BiasAddBiasAdddense_2/MatMul:product:0&dense_2/BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_2/BiasAddl
IdentityIdentitydense_2/BiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ:::::::T P
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
%
_user_specified_namedense_input
¬
ø
G__inference_sequential_1_layer_call_and_return_conditional_losses_13325
dense_3_input*
&dense_3_matmul_readvariableop_resource+
'dense_3_biasadd_readvariableop_resource*
&dense_4_matmul_readvariableop_resource+
'dense_4_biasadd_readvariableop_resource*
&dense_5_matmul_readvariableop_resource+
'dense_5_biasadd_readvariableop_resource
identity¦
dense_3/MatMul/ReadVariableOpReadVariableOp&dense_3_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_3/MatMul/ReadVariableOp
dense_3/MatMulMatMuldense_3_input%dense_3/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/MatMul¥
dense_3/BiasAdd/ReadVariableOpReadVariableOp'dense_3_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_3/BiasAdd/ReadVariableOp¢
dense_3/BiasAddBiasAdddense_3/MatMul:product:0&dense_3/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/BiasAddq
dense_3/ReluReludense_3/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/Relu§
dense_4/MatMul/ReadVariableOpReadVariableOp&dense_4_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
dense_4/MatMul/ReadVariableOp 
dense_4/MatMulMatMuldense_3/Relu:activations:0%dense_4/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/MatMul¥
dense_4/BiasAdd/ReadVariableOpReadVariableOp'dense_4_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_4/BiasAdd/ReadVariableOp¢
dense_4/BiasAddBiasAdddense_4/MatMul:product:0&dense_4/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/BiasAddq
dense_4/ReluReludense_4/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/Relu¦
dense_5/MatMul/ReadVariableOpReadVariableOp&dense_5_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_5/MatMul/ReadVariableOp
dense_5/MatMulMatMuldense_4/Relu:activations:0%dense_5/MatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/MatMul¤
dense_5/BiasAdd/ReadVariableOpReadVariableOp'dense_5_biasadd_readvariableop_resource*
_output_shapes
:*
dtype02 
dense_5/BiasAdd/ReadVariableOp¡
dense_5/BiasAddBiasAdddense_5/MatMul:product:0&dense_5/BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/BiasAddp
dense_5/TanhTanhdense_5/BiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/Tanhd
IdentityIdentitydense_5/Tanh:y:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ:::::::V R
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
'
_user_specified_namedense_3_input
¬
ø
G__inference_sequential_1_layer_call_and_return_conditional_losses_13350
dense_3_input*
&dense_3_matmul_readvariableop_resource+
'dense_3_biasadd_readvariableop_resource*
&dense_4_matmul_readvariableop_resource+
'dense_4_biasadd_readvariableop_resource*
&dense_5_matmul_readvariableop_resource+
'dense_5_biasadd_readvariableop_resource
identity¦
dense_3/MatMul/ReadVariableOpReadVariableOp&dense_3_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_3/MatMul/ReadVariableOp
dense_3/MatMulMatMuldense_3_input%dense_3/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/MatMul¥
dense_3/BiasAdd/ReadVariableOpReadVariableOp'dense_3_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_3/BiasAdd/ReadVariableOp¢
dense_3/BiasAddBiasAdddense_3/MatMul:product:0&dense_3/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/BiasAddq
dense_3/ReluReludense_3/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/Relu§
dense_4/MatMul/ReadVariableOpReadVariableOp&dense_4_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
dense_4/MatMul/ReadVariableOp 
dense_4/MatMulMatMuldense_3/Relu:activations:0%dense_4/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/MatMul¥
dense_4/BiasAdd/ReadVariableOpReadVariableOp'dense_4_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_4/BiasAdd/ReadVariableOp¢
dense_4/BiasAddBiasAdddense_4/MatMul:product:0&dense_4/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/BiasAddq
dense_4/ReluReludense_4/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/Relu¦
dense_5/MatMul/ReadVariableOpReadVariableOp&dense_5_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_5/MatMul/ReadVariableOp
dense_5/MatMulMatMuldense_4/Relu:activations:0%dense_5/MatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/MatMul¤
dense_5/BiasAdd/ReadVariableOpReadVariableOp'dense_5_biasadd_readvariableop_resource*
_output_shapes
:*
dtype02 
dense_5/BiasAdd/ReadVariableOp¡
dense_5/BiasAddBiasAdddense_5/MatMul:product:0&dense_5/BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/BiasAddp
dense_5/TanhTanhdense_5/BiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/Tanhd
IdentityIdentitydense_5/Tanh:y:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ:::::::V R
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
'
_user_specified_namedense_3_input
Ã

 __inference__wrapped_model_12781
dense_input3
/sequential_dense_matmul_readvariableop_resource4
0sequential_dense_biasadd_readvariableop_resource5
1sequential_dense_1_matmul_readvariableop_resource6
2sequential_dense_1_biasadd_readvariableop_resource5
1sequential_dense_2_matmul_readvariableop_resource6
2sequential_dense_2_biasadd_readvariableop_resource
identityÁ
&sequential/dense/MatMul/ReadVariableOpReadVariableOp/sequential_dense_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02(
&sequential/dense/MatMul/ReadVariableOp¬
sequential/dense/MatMulMatMuldense_input.sequential/dense/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
sequential/dense/MatMulÀ
'sequential/dense/BiasAdd/ReadVariableOpReadVariableOp0sequential_dense_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02)
'sequential/dense/BiasAdd/ReadVariableOpÆ
sequential/dense/BiasAddBiasAdd!sequential/dense/MatMul:product:0/sequential/dense/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
sequential/dense/BiasAdd
sequential/dense/ReluRelu!sequential/dense/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
sequential/dense/ReluÈ
(sequential/dense_1/MatMul/ReadVariableOpReadVariableOp1sequential_dense_1_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype02*
(sequential/dense_1/MatMul/ReadVariableOpÊ
sequential/dense_1/MatMulMatMul#sequential/dense/Relu:activations:00sequential/dense_1/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
sequential/dense_1/MatMulÆ
)sequential/dense_1/BiasAdd/ReadVariableOpReadVariableOp2sequential_dense_1_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02+
)sequential/dense_1/BiasAdd/ReadVariableOpÎ
sequential/dense_1/BiasAddBiasAdd#sequential/dense_1/MatMul:product:01sequential/dense_1/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
sequential/dense_1/BiasAdd
sequential/dense_1/ReluRelu#sequential/dense_1/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
sequential/dense_1/ReluÇ
(sequential/dense_2/MatMul/ReadVariableOpReadVariableOp1sequential_dense_2_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02*
(sequential/dense_2/MatMul/ReadVariableOpË
sequential/dense_2/MatMulMatMul%sequential/dense_1/Relu:activations:00sequential/dense_2/MatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
sequential/dense_2/MatMulÅ
)sequential/dense_2/BiasAdd/ReadVariableOpReadVariableOp2sequential_dense_2_biasadd_readvariableop_resource*
_output_shapes
:*
dtype02+
)sequential/dense_2/BiasAdd/ReadVariableOpÍ
sequential/dense_2/BiasAddBiasAdd#sequential/dense_2/MatMul:product:01sequential/dense_2/BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
sequential/dense_2/BiasAddw
IdentityIdentity#sequential/dense_2/BiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ:::::::T P
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
%
_user_specified_namedense_input
Ó
¦
cond_true_342
cond_placeholder
cond_placeholder_1
cond_placeholder_2
cond_placeholder_3
cond_placeholder_4
cond_placeholder_5
cond_placeholder_6
cond_placeholder_7
cond_placeholder_8
cond_placeholder_9
cond_placeholder_10	
cond_placeholder_11	
cond_placeholder_12
cond_placeholder_13
cond_placeholder_14
cond_placeholder_15
cond_placeholder_16
cond_placeholder_17
cond_placeholder_18
cond_placeholder_19
cond_placeholder_20
cond_identityy
cond/random_uniform/shapeConst*
_output_shapes
: *
dtype0*
valueB 2
cond/random_uniform/shapew
cond/random_uniform/minConst*
_output_shapes
: *
dtype0*
valueB
 *    2
cond/random_uniform/minw
cond/random_uniform/maxConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
cond/random_uniform/max©
!cond/random_uniform/RandomUniformRandomUniform"cond/random_uniform/shape:output:0*
T0*
_output_shapes
: *
dtype02#
!cond/random_uniform/RandomUniform
cond/random_uniform/subSub cond/random_uniform/max:output:0 cond/random_uniform/min:output:0*
T0*
_output_shapes
: 2
cond/random_uniform/sub£
cond/random_uniform/mulMul*cond/random_uniform/RandomUniform:output:0cond/random_uniform/sub:z:0*
T0*
_output_shapes
: 2
cond/random_uniform/mul
cond/random_uniformAddcond/random_uniform/mul:z:0 cond/random_uniform/min:output:0*
T0*
_output_shapes
: 2
cond/random_uniformU
cond/AbsAbscond/random_uniform:z:0*
T0*
_output_shapes
: 2

cond/AbsY
cond/IdentityIdentitycond/Abs:y:0*
T0*
_output_shapes
: 2
cond/Identity"'
cond_identitycond/Identity:output:0*p
_input_shapes_
]: : : : : : : : : : : : :ÿÿÿÿÿÿÿÿÿ:ÿÿÿÿÿÿÿÿÿ:ÿÿÿÿÿÿÿÿÿ::::::: 

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :	

_output_shapes
: :


_output_shapes
: :

_output_shapes
: :)%
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ:)%
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ:)%
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
ðd
ö
cond_false_343&
"cond_cast_finalfundamentalestimate
cond_cast_1_privatebid
cond_cast_2_privateask
cond_cast_3_omegaratiobid
cond_cast_4_omegaratioask
cond_cast_5_side
cond_cast_6_bidsize
cond_cast_7_asksize
cond_cast_8_spread
cond_cast_9_marketholdings
cond_cast_10_timetilend	#
cond_cast_11_timesincelasttrade	 
cond_strided_slice_bidvector"
cond_strided_slice_1_askvector+
'cond_strided_slice_2_transactionhistory=
9actor_sequential_1_dense_3_matmul_readvariableop_resource>
:actor_sequential_1_dense_3_biasadd_readvariableop_resource=
9actor_sequential_1_dense_4_matmul_readvariableop_resource>
:actor_sequential_1_dense_4_biasadd_readvariableop_resource=
9actor_sequential_1_dense_5_matmul_readvariableop_resource>
:actor_sequential_1_dense_5_biasadd_readvariableop_resource
cond_identityr
	cond/CastCast"cond_cast_finalfundamentalestimate*

DstT0*

SrcT0*
_output_shapes
: 2
	cond/Castj
cond/Cast_1Castcond_cast_1_privatebid*

DstT0*

SrcT0*
_output_shapes
: 2
cond/Cast_1j
cond/Cast_2Castcond_cast_2_privateask*

DstT0*

SrcT0*
_output_shapes
: 2
cond/Cast_2m
cond/Cast_3Castcond_cast_3_omegaratiobid*

DstT0*

SrcT0*
_output_shapes
: 2
cond/Cast_3m
cond/Cast_4Castcond_cast_4_omegaratioask*

DstT0*

SrcT0*
_output_shapes
: 2
cond/Cast_4d
cond/Cast_5Castcond_cast_5_side*

DstT0*

SrcT0*
_output_shapes
: 2
cond/Cast_5g
cond/Cast_6Castcond_cast_6_bidsize*

DstT0*

SrcT0*
_output_shapes
: 2
cond/Cast_6g
cond/Cast_7Castcond_cast_7_asksize*

DstT0*

SrcT0*
_output_shapes
: 2
cond/Cast_7f
cond/Cast_8Castcond_cast_8_spread*

DstT0*

SrcT0*
_output_shapes
: 2
cond/Cast_8n
cond/Cast_9Castcond_cast_9_marketholdings*

DstT0*

SrcT0*
_output_shapes
: 2
cond/Cast_9m
cond/Cast_10Castcond_cast_10_timetilend*

DstT0*

SrcT0	*
_output_shapes
: 2
cond/Cast_10u
cond/Cast_11Castcond_cast_11_timesincelasttrade*

DstT0*

SrcT0	*
_output_shapes
: 2
cond/Cast_11

cond/stackPackcond/Cast:y:0cond/Cast_1:y:0cond/Cast_2:y:0cond/Cast_3:y:0cond/Cast_4:y:0cond/Cast_5:y:0cond/Cast_6:y:0cond/Cast_7:y:0cond/Cast_8:y:0cond/Cast_9:y:0cond/Cast_10:y:0cond/Cast_11:y:0*
N*
T0*
_output_shapes
:2

cond/stacky
cond/Reshape/shapeConst*
_output_shapes
:*
dtype0*
valueB"      2
cond/Reshape/shape
cond/ReshapeReshapecond/stack:output:0cond/Reshape/shape:output:0*
T0*
_output_shapes

:2
cond/Reshape~
cond/strided_slice/stackConst*
_output_shapes
:*
dtype0*
valueB:2
cond/strided_slice/stack
cond/strided_slice/stack_1Const*
_output_shapes
:*
dtype0*
valueB:2
cond/strided_slice/stack_1
cond/strided_slice/stack_2Const*
_output_shapes
:*
dtype0*
valueB:2
cond/strided_slice/stack_2þ
cond/strided_sliceStridedSlicecond_strided_slice_bidvector!cond/strided_slice/stack:output:0#cond/strided_slice/stack_1:output:0#cond/strided_slice/stack_2:output:0*
Index0*
T0*#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
cond/strided_slice}
cond/Reshape_1/shapeConst*
_output_shapes
:*
dtype0*
valueB"   ÿÿÿÿ2
cond/Reshape_1/shape
cond/Reshape_1Reshapecond/strided_slice:output:0cond/Reshape_1/shape:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
cond/Reshape_1~
cond/Cast_12Castcond/Reshape_1:output:0*

DstT0*

SrcT0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
cond/Cast_12
cond/strided_slice_1/stackConst*
_output_shapes
:*
dtype0*
valueB: 2
cond/strided_slice_1/stack
cond/strided_slice_1/stack_1Const*
_output_shapes
:*
dtype0*
valueB:2
cond/strided_slice_1/stack_1
cond/strided_slice_1/stack_2Const*
_output_shapes
:*
dtype0*
valueB:2
cond/strided_slice_1/stack_2
cond/strided_slice_1StridedSlicecond_strided_slice_1_askvector#cond/strided_slice_1/stack:output:0%cond/strided_slice_1/stack_1:output:0%cond/strided_slice_1/stack_2:output:0*
Index0*
T0*#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*

begin_mask2
cond/strided_slice_1}
cond/Reshape_2/shapeConst*
_output_shapes
:*
dtype0*
valueB"   ÿÿÿÿ2
cond/Reshape_2/shape
cond/Reshape_2Reshapecond/strided_slice_1:output:0cond/Reshape_2/shape:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
cond/Reshape_2~
cond/Cast_13Castcond/Reshape_2:output:0*

DstT0*

SrcT0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
cond/Cast_13
cond/strided_slice_2/stackConst*
_output_shapes
:*
dtype0*
valueB: 2
cond/strided_slice_2/stack
cond/strided_slice_2/stack_1Const*
_output_shapes
:*
dtype0*
valueB:2
cond/strided_slice_2/stack_1
cond/strided_slice_2/stack_2Const*
_output_shapes
:*
dtype0*
valueB:2
cond/strided_slice_2/stack_2¥
cond/strided_slice_2StridedSlice'cond_strided_slice_2_transactionhistory#cond/strided_slice_2/stack:output:0%cond/strided_slice_2/stack_1:output:0%cond/strided_slice_2/stack_2:output:0*
Index0*
T0*#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*

begin_mask2
cond/strided_slice_2}
cond/Reshape_3/shapeConst*
_output_shapes
:*
dtype0*
valueB"   ÿÿÿÿ2
cond/Reshape_3/shape
cond/Reshape_3Reshapecond/strided_slice_2:output:0cond/Reshape_3/shape:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
cond/Reshape_3~
cond/Cast_14Castcond/Reshape_3:output:0*

DstT0*

SrcT0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
cond/Cast_14f
cond/concat/axisConst*
_output_shapes
: *
dtype0*
value	B :2
cond/concat/axisÉ
cond/concatConcatV2cond/Reshape:output:0cond/Cast_12:y:0cond/Cast_13:y:0cond/Cast_14:y:0cond/concat/axis:output:0*
N*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
cond/concat}
cond/Reshape_4/shapeConst*
_output_shapes
:*
dtype0*
valueB"      2
cond/Reshape_4/shape
cond/Reshape_4Reshapecond/concat:output:0cond/Reshape_4/shape:output:0*
T0*
_output_shapes

:2
cond/Reshape_4ä
cond/Reshape_5/tensorConst*
_output_shapes
:*
dtype0*
valuexBv"lOPÃGYÃ`ÍC8Fõ/EéºÌ6ÓB¤@ÓB¾ØC    ¢yDd¬C¡DÄã3Ä+u"ÄÖ¾ÄRCïÃ×Ð ÄÅðÄÿ¶+Ä÷&=ÄÁâMÄl¨Ãº¸ÃËìÃÁÏÃÙÃ2
cond/Reshape_5/tensor}
cond/Reshape_5/shapeConst*
_output_shapes
:*
dtype0*
valueB"   ÿÿÿÿ2
cond/Reshape_5/shape
cond/Reshape_5Reshapecond/Reshape_5/tensor:output:0cond/Reshape_5/shape:output:0*
T0*
_output_shapes

:2
cond/Reshape_5ä
cond/Reshape_6/tensorConst*
_output_shapes
:*
dtype0*
valuexBv"l6ÁF]é»Iþå»IS¯+Mx>ÉLûÿ?H4ÞBtÞBÉ¯ÜF    ØU¢HÔHs¿I¾J¿Iò¿IkÚ¾IÜ¾I<¾I7¾Iç¾Iþ1¿I^¿IkäÃI³NÄI«{ÅIÎºÅI×ÅI2
cond/Reshape_6/tensor}
cond/Reshape_6/shapeConst*
_output_shapes
:*
dtype0*
valueB"   ÿÿÿÿ2
cond/Reshape_6/shape
cond/Reshape_6Reshapecond/Reshape_6/tensor:output:0cond/Reshape_6/shape:output:0*
T0*
_output_shapes

:2
cond/Reshape_6`
	cond/SqrtSqrtcond/Reshape_6:output:0*
T0*
_output_shapes

:2
	cond/Sqrtv
cond/SubSubcond/Reshape_4:output:0cond/Reshape_5:output:0*
T0*
_output_shapes

:2

cond/Subt
cond/div_no_nanDivNoNancond/Sub:z:0cond/Sqrt:y:0*
T0*
_output_shapes

:2
cond/div_no_nanß
0actor/sequential_1/dense_3/MatMul/ReadVariableOpReadVariableOp9actor_sequential_1_dense_3_matmul_readvariableop_resource*
_output_shapes
:	*
dtype022
0actor/sequential_1/dense_3/MatMul/ReadVariableOpÉ
!actor/sequential_1/dense_3/MatMulMatMulcond/div_no_nan:z:08actor/sequential_1/dense_3/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	2#
!actor/sequential_1/dense_3/MatMulÞ
1actor/sequential_1/dense_3/BiasAdd/ReadVariableOpReadVariableOp:actor_sequential_1_dense_3_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype023
1actor/sequential_1/dense_3/BiasAdd/ReadVariableOpå
"actor/sequential_1/dense_3/BiasAddBiasAdd+actor/sequential_1/dense_3/MatMul:product:09actor/sequential_1/dense_3/BiasAdd/ReadVariableOp:value:0*
T0*
_output_shapes
:	2$
"actor/sequential_1/dense_3/BiasAdd¡
actor/sequential_1/dense_3/ReluRelu+actor/sequential_1/dense_3/BiasAdd:output:0*
T0*
_output_shapes
:	2!
actor/sequential_1/dense_3/Reluà
0actor/sequential_1/dense_4/MatMul/ReadVariableOpReadVariableOp9actor_sequential_1_dense_4_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype022
0actor/sequential_1/dense_4/MatMul/ReadVariableOpã
!actor/sequential_1/dense_4/MatMulMatMul-actor/sequential_1/dense_3/Relu:activations:08actor/sequential_1/dense_4/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	2#
!actor/sequential_1/dense_4/MatMulÞ
1actor/sequential_1/dense_4/BiasAdd/ReadVariableOpReadVariableOp:actor_sequential_1_dense_4_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype023
1actor/sequential_1/dense_4/BiasAdd/ReadVariableOpå
"actor/sequential_1/dense_4/BiasAddBiasAdd+actor/sequential_1/dense_4/MatMul:product:09actor/sequential_1/dense_4/BiasAdd/ReadVariableOp:value:0*
T0*
_output_shapes
:	2$
"actor/sequential_1/dense_4/BiasAdd¡
actor/sequential_1/dense_4/ReluRelu+actor/sequential_1/dense_4/BiasAdd:output:0*
T0*
_output_shapes
:	2!
actor/sequential_1/dense_4/Reluß
0actor/sequential_1/dense_5/MatMul/ReadVariableOpReadVariableOp9actor_sequential_1_dense_5_matmul_readvariableop_resource*
_output_shapes
:	*
dtype022
0actor/sequential_1/dense_5/MatMul/ReadVariableOpâ
!actor/sequential_1/dense_5/MatMulMatMul-actor/sequential_1/dense_4/Relu:activations:08actor/sequential_1/dense_5/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes

:2#
!actor/sequential_1/dense_5/MatMulÝ
1actor/sequential_1/dense_5/BiasAdd/ReadVariableOpReadVariableOp:actor_sequential_1_dense_5_biasadd_readvariableop_resource*
_output_shapes
:*
dtype023
1actor/sequential_1/dense_5/BiasAdd/ReadVariableOpä
"actor/sequential_1/dense_5/BiasAddBiasAdd+actor/sequential_1/dense_5/MatMul:product:09actor/sequential_1/dense_5/BiasAdd/ReadVariableOp:value:0*
T0*
_output_shapes

:2$
"actor/sequential_1/dense_5/BiasAdd 
actor/sequential_1/dense_5/TanhTanh+actor/sequential_1/dense_5/BiasAdd:output:0*
T0*
_output_shapes

:2!
actor/sequential_1/dense_5/Tanhw
cond/random_normal/shapeConst*
_output_shapes
: *
dtype0*
valueB 2
cond/random_normal/shapew
cond/random_normal/meanConst*
_output_shapes
: *
dtype0*
valueB
 *    2
cond/random_normal/mean{
cond/random_normal/stddevConst*
_output_shapes
: *
dtype0*
valueB
 *ÍÌÌ=2
cond/random_normal/stddev»
'cond/random_normal/RandomStandardNormalRandomStandardNormal!cond/random_normal/shape:output:0*
T0*
_output_shapes
: *
dtype02)
'cond/random_normal/RandomStandardNormal®
cond/random_normal/mulMul0cond/random_normal/RandomStandardNormal:output:0"cond/random_normal/stddev:output:0*
T0*
_output_shapes
: 2
cond/random_normal/mul
cond/random_normalAddcond/random_normal/mul:z:0 cond/random_normal/mean:output:0*
T0*
_output_shapes
: 2
cond/random_normal
cond/addAddV2#actor/sequential_1/dense_5/Tanh:y:0cond/random_normal:z:0*
T0*
_output_shapes

:2

cond/adda
cond/IdentityIdentitycond/add:z:0*
T0*
_output_shapes

:2
cond/Identity"'
cond_identitycond/Identity:output:0*p
_input_shapes_
]: : : : : : : : : : : : :ÿÿÿÿÿÿÿÿÿ:ÿÿÿÿÿÿÿÿÿ:ÿÿÿÿÿÿÿÿÿ::::::: 

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :	

_output_shapes
: :


_output_shapes
: :

_output_shapes
: :)%
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ:)%
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ:)%
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ

ë
E__inference_sequential_layer_call_and_return_conditional_losses_13160

inputs(
$dense_matmul_readvariableop_resource)
%dense_biasadd_readvariableop_resource*
&dense_1_matmul_readvariableop_resource+
'dense_1_biasadd_readvariableop_resource*
&dense_2_matmul_readvariableop_resource+
'dense_2_biasadd_readvariableop_resource
identity 
dense/MatMul/ReadVariableOpReadVariableOp$dense_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense/MatMul/ReadVariableOp
dense/MatMulMatMulinputs#dense/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense/MatMul
dense/BiasAdd/ReadVariableOpReadVariableOp%dense_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
dense/BiasAdd/ReadVariableOp
dense/BiasAddBiasAdddense/MatMul:product:0$dense/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense/BiasAddk

dense/ReluReludense/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

dense/Relu§
dense_1/MatMul/ReadVariableOpReadVariableOp&dense_1_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
dense_1/MatMul/ReadVariableOp
dense_1/MatMulMatMuldense/Relu:activations:0%dense_1/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/MatMul¥
dense_1/BiasAdd/ReadVariableOpReadVariableOp'dense_1_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_1/BiasAdd/ReadVariableOp¢
dense_1/BiasAddBiasAdddense_1/MatMul:product:0&dense_1/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/BiasAddq
dense_1/ReluReludense_1/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/Relu¦
dense_2/MatMul/ReadVariableOpReadVariableOp&dense_2_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_2/MatMul/ReadVariableOp
dense_2/MatMulMatMuldense_1/Relu:activations:0%dense_2/MatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_2/MatMul¤
dense_2/BiasAdd/ReadVariableOpReadVariableOp'dense_2_biasadd_readvariableop_resource*
_output_shapes
:*
dtype02 
dense_2/BiasAdd/ReadVariableOp¡
dense_2/BiasAddBiasAdddense_2/MatMul:product:0&dense_2/BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_2/BiasAddl
IdentityIdentitydense_2/BiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ:::::::O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
«
¨
@__inference_dense_layer_call_and_return_conditional_losses_12796

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource*
_output_shapes
:	*
dtype02
MatMul/ReadVariableOpt
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddY
ReluReluBiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Relug
IdentityIdentityRelu:activations:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*.
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs

ë
E__inference_sequential_layer_call_and_return_conditional_losses_13184

inputs(
$dense_matmul_readvariableop_resource)
%dense_biasadd_readvariableop_resource*
&dense_1_matmul_readvariableop_resource+
'dense_1_biasadd_readvariableop_resource*
&dense_2_matmul_readvariableop_resource+
'dense_2_biasadd_readvariableop_resource
identity 
dense/MatMul/ReadVariableOpReadVariableOp$dense_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense/MatMul/ReadVariableOp
dense/MatMulMatMulinputs#dense/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense/MatMul
dense/BiasAdd/ReadVariableOpReadVariableOp%dense_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
dense/BiasAdd/ReadVariableOp
dense/BiasAddBiasAdddense/MatMul:product:0$dense/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense/BiasAddk

dense/ReluReludense/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

dense/Relu§
dense_1/MatMul/ReadVariableOpReadVariableOp&dense_1_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
dense_1/MatMul/ReadVariableOp
dense_1/MatMulMatMuldense/Relu:activations:0%dense_1/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/MatMul¥
dense_1/BiasAdd/ReadVariableOpReadVariableOp'dense_1_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_1/BiasAdd/ReadVariableOp¢
dense_1/BiasAddBiasAdddense_1/MatMul:product:0&dense_1/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/BiasAddq
dense_1/ReluReludense_1/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/Relu¦
dense_2/MatMul/ReadVariableOpReadVariableOp&dense_2_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_2/MatMul/ReadVariableOp
dense_2/MatMulMatMuldense_1/Relu:activations:0%dense_2/MatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_2/MatMul¤
dense_2/BiasAdd/ReadVariableOpReadVariableOp'dense_2_biasadd_readvariableop_resource*
_output_shapes
:*
dtype02 
dense_2/BiasAdd/ReadVariableOp¡
dense_2/BiasAddBiasAdddense_2/MatMul:product:0&dense_2/BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_2/BiasAddl
IdentityIdentitydense_2/BiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ:::::::O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs

ð
E__inference_sequential_layer_call_and_return_conditional_losses_13242
dense_input(
$dense_matmul_readvariableop_resource)
%dense_biasadd_readvariableop_resource*
&dense_1_matmul_readvariableop_resource+
'dense_1_biasadd_readvariableop_resource*
&dense_2_matmul_readvariableop_resource+
'dense_2_biasadd_readvariableop_resource
identity 
dense/MatMul/ReadVariableOpReadVariableOp$dense_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense/MatMul/ReadVariableOp
dense/MatMulMatMuldense_input#dense/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense/MatMul
dense/BiasAdd/ReadVariableOpReadVariableOp%dense_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
dense/BiasAdd/ReadVariableOp
dense/BiasAddBiasAdddense/MatMul:product:0$dense/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense/BiasAddk

dense/ReluReludense/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

dense/Relu§
dense_1/MatMul/ReadVariableOpReadVariableOp&dense_1_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
dense_1/MatMul/ReadVariableOp
dense_1/MatMulMatMuldense/Relu:activations:0%dense_1/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/MatMul¥
dense_1/BiasAdd/ReadVariableOpReadVariableOp'dense_1_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_1/BiasAdd/ReadVariableOp¢
dense_1/BiasAddBiasAdddense_1/MatMul:product:0&dense_1/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/BiasAddq
dense_1/ReluReludense_1/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_1/Relu¦
dense_2/MatMul/ReadVariableOpReadVariableOp&dense_2_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_2/MatMul/ReadVariableOp
dense_2/MatMulMatMuldense_1/Relu:activations:0%dense_2/MatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_2/MatMul¤
dense_2/BiasAdd/ReadVariableOpReadVariableOp'dense_2_biasadd_readvariableop_resource*
_output_shapes
:*
dtype02 
dense_2/BiasAdd/ReadVariableOp¡
dense_2/BiasAddBiasAdddense_2/MatMul:product:0&dense_2/BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_2/BiasAddl
IdentityIdentitydense_2/BiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ:::::::T P
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
%
_user_specified_namedense_input
ø
Ä
,__inference_sequential_1_layer_call_fn_13367
dense_3_input
unknown
	unknown_0
	unknown_1
	unknown_2
	unknown_3
	unknown_4
identity¢StatefulPartitionedCall²
StatefulPartitionedCallStatefulPartitionedCalldense_3_inputunknown	unknown_0	unknown_1	unknown_2	unknown_3	unknown_4*
Tin
	2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 *P
fKRI
G__inference_sequential_1_layer_call_and_return_conditional_losses_130852
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::22
StatefulPartitionedCallStatefulPartitionedCall:V R
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
'
_user_specified_namedense_3_input
°
ª
B__inference_dense_4_layer_call_and_return_conditional_losses_13000

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
MatMul/ReadVariableOpt
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddY
ReluReluBiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Relug
IdentityIdentityRelu:activations:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
«
¨
@__inference_dense_layer_call_and_return_conditional_losses_13479

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource*
_output_shapes
:	*
dtype02
MatMul/ReadVariableOpt
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddY
ReluReluBiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Relug
IdentityIdentityRelu:activations:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*.
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
Î
ª
B__inference_dense_2_layer_call_and_return_conditional_losses_12849

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource*
_output_shapes
:	*
dtype02
MatMul/ReadVariableOps
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddd
IdentityIdentityBiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
ø
Ä
,__inference_sequential_1_layer_call_fn_13384
dense_3_input
unknown
	unknown_0
	unknown_1
	unknown_2
	unknown_3
	unknown_4
identity¢StatefulPartitionedCall²
StatefulPartitionedCallStatefulPartitionedCalldense_3_inputunknown	unknown_0	unknown_1	unknown_2	unknown_3	unknown_4*
Tin
	2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 *P
fKRI
G__inference_sequential_1_layer_call_and_return_conditional_losses_131212
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::22
StatefulPartitionedCallStatefulPartitionedCall:V R
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
'
_user_specified_namedense_3_input
î
À
*__inference_sequential_layer_call_fn_13300
dense_input
unknown
	unknown_0
	unknown_1
	unknown_2
	unknown_3
	unknown_4
identity¢StatefulPartitionedCall®
StatefulPartitionedCallStatefulPartitionedCalldense_inputunknown	unknown_0	unknown_1	unknown_2	unknown_3	unknown_4*
Tin
	2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 *N
fIRG
E__inference_sequential_layer_call_and_return_conditional_losses_129432
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::22
StatefulPartitionedCallStatefulPartitionedCall:T P
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
%
_user_specified_namedense_input
û
¶
E__inference_sequential_layer_call_and_return_conditional_losses_12907

inputs
dense_12891
dense_12893
dense_1_12896
dense_1_12898
dense_2_12901
dense_2_12903
identity¢dense/StatefulPartitionedCall¢dense_1/StatefulPartitionedCall¢dense_2/StatefulPartitionedCall
dense/StatefulPartitionedCallStatefulPartitionedCallinputsdense_12891dense_12893*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *I
fDRB
@__inference_dense_layer_call_and_return_conditional_losses_127962
dense/StatefulPartitionedCall­
dense_1/StatefulPartitionedCallStatefulPartitionedCall&dense/StatefulPartitionedCall:output:0dense_1_12896dense_1_12898*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_1_layer_call_and_return_conditional_losses_128232!
dense_1/StatefulPartitionedCall®
dense_2/StatefulPartitionedCallStatefulPartitionedCall(dense_1/StatefulPartitionedCall:output:0dense_2_12901dense_2_12903*
Tin
2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_2_layer_call_and_return_conditional_losses_128492!
dense_2/StatefulPartitionedCallà
IdentityIdentity(dense_2/StatefulPartitionedCall:output:0^dense/StatefulPartitionedCall ^dense_1/StatefulPartitionedCall ^dense_2/StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::2>
dense/StatefulPartitionedCalldense/StatefulPartitionedCall2B
dense_1/StatefulPartitionedCalldense_1/StatefulPartitionedCall2B
dense_2/StatefulPartitionedCalldense_2/StatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs

Ã
__inference__training_step_7508
transitions_0
transitions_1
transitions_2
transitions_3
transitions_4	=
9actor_sequential_1_dense_3_matmul_readvariableop_resource>
:actor_sequential_1_dense_3_biasadd_readvariableop_resource=
9actor_sequential_1_dense_4_matmul_readvariableop_resource>
:actor_sequential_1_dense_4_biasadd_readvariableop_resource=
9actor_sequential_1_dense_5_matmul_readvariableop_resource>
:actor_sequential_1_dense_5_biasadd_readvariableop_resource:
6critic_sequential_dense_matmul_readvariableop_resource;
7critic_sequential_dense_biasadd_readvariableop_resource<
8critic_sequential_dense_1_matmul_readvariableop_resource=
9critic_sequential_dense_1_biasadd_readvariableop_resource<
8critic_sequential_dense_2_matmul_readvariableop_resource=
9critic_sequential_dense_2_biasadd_readvariableop_resource%
!adam_assignaddvariableop_resource$
 adam_mul_readvariableop_resource&
"adam_mul_2_readvariableop_resource&
"adam_mul_6_readvariableop_resource&
"adam_mul_8_readvariableop_resource'
#adam_mul_12_readvariableop_resource'
#adam_mul_14_readvariableop_resource'
#adam_mul_18_readvariableop_resource'
#adam_mul_20_readvariableop_resource'
#adam_mul_24_readvariableop_resource'
#adam_mul_26_readvariableop_resource'
#adam_mul_30_readvariableop_resource'
#adam_mul_32_readvariableop_resource'
#adam_assignaddvariableop_1_resource'
#adam_mul_36_readvariableop_resource'
#adam_mul_38_readvariableop_resource'
#adam_mul_42_readvariableop_resource'
#adam_mul_44_readvariableop_resource'
#adam_mul_48_readvariableop_resource'
#adam_mul_50_readvariableop_resource'
#adam_mul_54_readvariableop_resource'
#adam_mul_56_readvariableop_resource'
#adam_mul_60_readvariableop_resource'
#adam_mul_62_readvariableop_resource'
#adam_mul_66_readvariableop_resource'
#adam_mul_68_readvariableop_resource
identity

identity_1¢adam/AssignAddVariableOp¢adam/AssignAddVariableOp_1¢adam/AssignSubVariableOp¢adam/AssignSubVariableOp_1¢adam/AssignSubVariableOp_10¢adam/AssignSubVariableOp_11¢adam/AssignSubVariableOp_2¢adam/AssignSubVariableOp_3¢adam/AssignSubVariableOp_4¢adam/AssignSubVariableOp_5¢adam/AssignSubVariableOp_6¢adam/AssignSubVariableOp_7¢adam/AssignSubVariableOp_8¢adam/AssignSubVariableOp_9¢adam/AssignVariableOp¢adam/AssignVariableOp_1¢adam/AssignVariableOp_10¢adam/AssignVariableOp_11¢adam/AssignVariableOp_12¢adam/AssignVariableOp_13¢adam/AssignVariableOp_14¢adam/AssignVariableOp_15¢adam/AssignVariableOp_16¢adam/AssignVariableOp_17¢adam/AssignVariableOp_18¢adam/AssignVariableOp_19¢adam/AssignVariableOp_2¢adam/AssignVariableOp_20¢adam/AssignVariableOp_21¢adam/AssignVariableOp_22¢adam/AssignVariableOp_23¢adam/AssignVariableOp_3¢adam/AssignVariableOp_4¢adam/AssignVariableOp_5¢adam/AssignVariableOp_6¢adam/AssignVariableOp_7¢adam/AssignVariableOp_8¢adam/AssignVariableOp_9W
CastCasttransitions_1*

DstT0*

SrcT0*
_output_shapes
: 2
Cast[
Cast_1Casttransitions_3*

DstT0*

SrcT0*
_output_shapes
: 2
Cast_1[
Cast_2Casttransitions_4*

DstT0*

SrcT0	*
_output_shapes
: 2
Cast_2ß
0actor/sequential_1/dense_3/MatMul/ReadVariableOpReadVariableOp9actor_sequential_1_dense_3_matmul_readvariableop_resource*
_output_shapes
:	*
dtype022
0actor/sequential_1/dense_3/MatMul/ReadVariableOpÃ
!actor/sequential_1/dense_3/MatMulMatMultransitions_28actor/sequential_1/dense_3/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2#
!actor/sequential_1/dense_3/MatMulÞ
1actor/sequential_1/dense_3/BiasAdd/ReadVariableOpReadVariableOp:actor_sequential_1_dense_3_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype023
1actor/sequential_1/dense_3/BiasAdd/ReadVariableOpå
"actor/sequential_1/dense_3/BiasAddBiasAdd+actor/sequential_1/dense_3/MatMul:product:09actor/sequential_1/dense_3/BiasAdd/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2$
"actor/sequential_1/dense_3/BiasAdd¡
actor/sequential_1/dense_3/ReluRelu+actor/sequential_1/dense_3/BiasAdd:output:0*
T0*
_output_shapes
:	 2!
actor/sequential_1/dense_3/Reluà
0actor/sequential_1/dense_4/MatMul/ReadVariableOpReadVariableOp9actor_sequential_1_dense_4_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype022
0actor/sequential_1/dense_4/MatMul/ReadVariableOpã
!actor/sequential_1/dense_4/MatMulMatMul-actor/sequential_1/dense_3/Relu:activations:08actor/sequential_1/dense_4/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2#
!actor/sequential_1/dense_4/MatMulÞ
1actor/sequential_1/dense_4/BiasAdd/ReadVariableOpReadVariableOp:actor_sequential_1_dense_4_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype023
1actor/sequential_1/dense_4/BiasAdd/ReadVariableOpå
"actor/sequential_1/dense_4/BiasAddBiasAdd+actor/sequential_1/dense_4/MatMul:product:09actor/sequential_1/dense_4/BiasAdd/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2$
"actor/sequential_1/dense_4/BiasAdd¡
actor/sequential_1/dense_4/ReluRelu+actor/sequential_1/dense_4/BiasAdd:output:0*
T0*
_output_shapes
:	 2!
actor/sequential_1/dense_4/Reluß
0actor/sequential_1/dense_5/MatMul/ReadVariableOpReadVariableOp9actor_sequential_1_dense_5_matmul_readvariableop_resource*
_output_shapes
:	*
dtype022
0actor/sequential_1/dense_5/MatMul/ReadVariableOpâ
!actor/sequential_1/dense_5/MatMulMatMul-actor/sequential_1/dense_4/Relu:activations:08actor/sequential_1/dense_5/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes

: 2#
!actor/sequential_1/dense_5/MatMulÝ
1actor/sequential_1/dense_5/BiasAdd/ReadVariableOpReadVariableOp:actor_sequential_1_dense_5_biasadd_readvariableop_resource*
_output_shapes
:*
dtype023
1actor/sequential_1/dense_5/BiasAdd/ReadVariableOpä
"actor/sequential_1/dense_5/BiasAddBiasAdd+actor/sequential_1/dense_5/MatMul:product:09actor/sequential_1/dense_5/BiasAdd/ReadVariableOp:value:0*
T0*
_output_shapes

: 2$
"actor/sequential_1/dense_5/BiasAdd 
actor/sequential_1/dense_5/TanhTanh+actor/sequential_1/dense_5/BiasAdd:output:0*
T0*
_output_shapes

: 2!
actor/sequential_1/dense_5/Tanhj
critic/concat/axisConst*
_output_shapes
: *
dtype0*
value	B :2
critic/concat/axis­
critic/concatConcatV2transitions_2#actor/sequential_1/dense_5/Tanh:y:0critic/concat/axis:output:0*
N*
T0*
_output_shapes

: 2
critic/concatÖ
-critic/sequential/dense/MatMul/ReadVariableOpReadVariableOp6critic_sequential_dense_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02/
-critic/sequential/dense/MatMul/ReadVariableOpÃ
critic/sequential/dense/MatMulMatMulcritic/concat:output:05critic/sequential/dense/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2 
critic/sequential/dense/MatMulÕ
.critic/sequential/dense/BiasAdd/ReadVariableOpReadVariableOp7critic_sequential_dense_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype020
.critic/sequential/dense/BiasAdd/ReadVariableOpÙ
critic/sequential/dense/BiasAddBiasAdd(critic/sequential/dense/MatMul:product:06critic/sequential/dense/BiasAdd/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2!
critic/sequential/dense/BiasAdd
critic/sequential/dense/ReluRelu(critic/sequential/dense/BiasAdd:output:0*
T0*
_output_shapes
:	 2
critic/sequential/dense/ReluÝ
/critic/sequential/dense_1/MatMul/ReadVariableOpReadVariableOp8critic_sequential_dense_1_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype021
/critic/sequential/dense_1/MatMul/ReadVariableOpÝ
 critic/sequential/dense_1/MatMulMatMul*critic/sequential/dense/Relu:activations:07critic/sequential/dense_1/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2"
 critic/sequential/dense_1/MatMulÛ
0critic/sequential/dense_1/BiasAdd/ReadVariableOpReadVariableOp9critic_sequential_dense_1_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype022
0critic/sequential/dense_1/BiasAdd/ReadVariableOpá
!critic/sequential/dense_1/BiasAddBiasAdd*critic/sequential/dense_1/MatMul:product:08critic/sequential/dense_1/BiasAdd/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2#
!critic/sequential/dense_1/BiasAdd
critic/sequential/dense_1/ReluRelu*critic/sequential/dense_1/BiasAdd:output:0*
T0*
_output_shapes
:	 2 
critic/sequential/dense_1/ReluÜ
/critic/sequential/dense_2/MatMul/ReadVariableOpReadVariableOp8critic_sequential_dense_2_matmul_readvariableop_resource*
_output_shapes
:	*
dtype021
/critic/sequential/dense_2/MatMul/ReadVariableOpÞ
 critic/sequential/dense_2/MatMulMatMul,critic/sequential/dense_1/Relu:activations:07critic/sequential/dense_2/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes

: 2"
 critic/sequential/dense_2/MatMulÚ
0critic/sequential/dense_2/BiasAdd/ReadVariableOpReadVariableOp9critic_sequential_dense_2_biasadd_readvariableop_resource*
_output_shapes
:*
dtype022
0critic/sequential/dense_2/BiasAdd/ReadVariableOpà
!critic/sequential/dense_2/BiasAddBiasAdd*critic/sequential/dense_2/MatMul:product:08critic/sequential/dense_2/BiasAdd/ReadVariableOp:value:0*
T0*
_output_shapes

: 2#
!critic/sequential/dense_2/BiasAdd
SqueezeSqueeze*critic/sequential/dense_2/BiasAdd:output:0*
T0*
_output_shapes
: *
squeeze_dims
2	
Squeezek
ExpandDims/dimConst*
_output_shapes
: *
dtype0*
valueB :
ÿÿÿÿÿÿÿÿÿ2
ExpandDims/dimr

ExpandDims
ExpandDimsCast:y:0ExpandDims/dim:output:0*
T0*
_output_shapes

: 2

ExpandDimsn
critic/concat_1/axisConst*
_output_shapes
: *
dtype0*
value	B :2
critic/concat_1/axis£
critic/concat_1ConcatV2transitions_0ExpandDims:output:0critic/concat_1/axis:output:0*
N*
T0*
_output_shapes

: 2
critic/concat_1Ú
/critic/sequential/dense/MatMul_1/ReadVariableOpReadVariableOp6critic_sequential_dense_matmul_readvariableop_resource*
_output_shapes
:	*
dtype021
/critic/sequential/dense/MatMul_1/ReadVariableOpË
 critic/sequential/dense/MatMul_1MatMulcritic/concat_1:output:07critic/sequential/dense/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2"
 critic/sequential/dense/MatMul_1Ù
0critic/sequential/dense/BiasAdd_1/ReadVariableOpReadVariableOp7critic_sequential_dense_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype022
0critic/sequential/dense/BiasAdd_1/ReadVariableOpá
!critic/sequential/dense/BiasAdd_1BiasAdd*critic/sequential/dense/MatMul_1:product:08critic/sequential/dense/BiasAdd_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2#
!critic/sequential/dense/BiasAdd_1
critic/sequential/dense/Relu_1Relu*critic/sequential/dense/BiasAdd_1:output:0*
T0*
_output_shapes
:	 2 
critic/sequential/dense/Relu_1á
1critic/sequential/dense_1/MatMul_1/ReadVariableOpReadVariableOp8critic_sequential_dense_1_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype023
1critic/sequential/dense_1/MatMul_1/ReadVariableOpå
"critic/sequential/dense_1/MatMul_1MatMul,critic/sequential/dense/Relu_1:activations:09critic/sequential/dense_1/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2$
"critic/sequential/dense_1/MatMul_1ß
2critic/sequential/dense_1/BiasAdd_1/ReadVariableOpReadVariableOp9critic_sequential_dense_1_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype024
2critic/sequential/dense_1/BiasAdd_1/ReadVariableOpé
#critic/sequential/dense_1/BiasAdd_1BiasAdd,critic/sequential/dense_1/MatMul_1:product:0:critic/sequential/dense_1/BiasAdd_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2%
#critic/sequential/dense_1/BiasAdd_1¤
 critic/sequential/dense_1/Relu_1Relu,critic/sequential/dense_1/BiasAdd_1:output:0*
T0*
_output_shapes
:	 2"
 critic/sequential/dense_1/Relu_1à
1critic/sequential/dense_2/MatMul_1/ReadVariableOpReadVariableOp8critic_sequential_dense_2_matmul_readvariableop_resource*
_output_shapes
:	*
dtype023
1critic/sequential/dense_2/MatMul_1/ReadVariableOpæ
"critic/sequential/dense_2/MatMul_1MatMul.critic/sequential/dense_1/Relu_1:activations:09critic/sequential/dense_2/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes

: 2$
"critic/sequential/dense_2/MatMul_1Þ
2critic/sequential/dense_2/BiasAdd_1/ReadVariableOpReadVariableOp9critic_sequential_dense_2_biasadd_readvariableop_resource*
_output_shapes
:*
dtype024
2critic/sequential/dense_2/BiasAdd_1/ReadVariableOpè
#critic/sequential/dense_2/BiasAdd_1BiasAdd,critic/sequential/dense_2/MatMul_1:product:0:critic/sequential/dense_2/BiasAdd_1/ReadVariableOp:value:0*
T0*
_output_shapes

: 2%
#critic/sequential/dense_2/BiasAdd_1
	Squeeze_1Squeeze,critic/sequential/dense_2/BiasAdd_1:output:0*
T0*
_output_shapes
: *
squeeze_dims
2
	Squeeze_1S
mul/xConst*
_output_shapes
: *
dtype0*
valueB
 *¤p}?2
mul/xX
mulMulmul/x:output:0Squeeze:output:0*
T0*
_output_shapes
: 2
mulS
sub/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
sub/xR
subSubsub/x:output:0
Cast_2:y:0*
T0*
_output_shapes
: 2
subL
mul_1Mulmul:z:0sub:z:0*
T0*
_output_shapes
: 2
mul_1O
addAddV2
Cast_1:y:0	mul_1:z:0*
T0*
_output_shapes
: 2
add}
SquaredDifferenceSquaredDifferenceSqueeze_1:output:0add:z:0*
T0*
_output_shapes
: 2
SquaredDifference{
Mean/reduction_indicesConst*
_output_shapes
: *
dtype0*
valueB :
ÿÿÿÿÿÿÿÿÿ2
Mean/reduction_indicesm
MeanMeanSquaredDifference:z:0Mean/reduction_indices:output:0*
T0*
_output_shapes
: 2
Meanã
2actor/sequential_1/dense_3/MatMul_1/ReadVariableOpReadVariableOp9actor_sequential_1_dense_3_matmul_readvariableop_resource*
_output_shapes
:	*
dtype024
2actor/sequential_1/dense_3/MatMul_1/ReadVariableOpÉ
#actor/sequential_1/dense_3/MatMul_1MatMultransitions_0:actor/sequential_1/dense_3/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2%
#actor/sequential_1/dense_3/MatMul_1â
3actor/sequential_1/dense_3/BiasAdd_1/ReadVariableOpReadVariableOp:actor_sequential_1_dense_3_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype025
3actor/sequential_1/dense_3/BiasAdd_1/ReadVariableOpí
$actor/sequential_1/dense_3/BiasAdd_1BiasAdd-actor/sequential_1/dense_3/MatMul_1:product:0;actor/sequential_1/dense_3/BiasAdd_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2&
$actor/sequential_1/dense_3/BiasAdd_1§
!actor/sequential_1/dense_3/Relu_1Relu-actor/sequential_1/dense_3/BiasAdd_1:output:0*
T0*
_output_shapes
:	 2#
!actor/sequential_1/dense_3/Relu_1ä
2actor/sequential_1/dense_4/MatMul_1/ReadVariableOpReadVariableOp9actor_sequential_1_dense_4_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype024
2actor/sequential_1/dense_4/MatMul_1/ReadVariableOpë
#actor/sequential_1/dense_4/MatMul_1MatMul/actor/sequential_1/dense_3/Relu_1:activations:0:actor/sequential_1/dense_4/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2%
#actor/sequential_1/dense_4/MatMul_1â
3actor/sequential_1/dense_4/BiasAdd_1/ReadVariableOpReadVariableOp:actor_sequential_1_dense_4_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype025
3actor/sequential_1/dense_4/BiasAdd_1/ReadVariableOpí
$actor/sequential_1/dense_4/BiasAdd_1BiasAdd-actor/sequential_1/dense_4/MatMul_1:product:0;actor/sequential_1/dense_4/BiasAdd_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2&
$actor/sequential_1/dense_4/BiasAdd_1§
!actor/sequential_1/dense_4/Relu_1Relu-actor/sequential_1/dense_4/BiasAdd_1:output:0*
T0*
_output_shapes
:	 2#
!actor/sequential_1/dense_4/Relu_1ã
2actor/sequential_1/dense_5/MatMul_1/ReadVariableOpReadVariableOp9actor_sequential_1_dense_5_matmul_readvariableop_resource*
_output_shapes
:	*
dtype024
2actor/sequential_1/dense_5/MatMul_1/ReadVariableOpê
#actor/sequential_1/dense_5/MatMul_1MatMul/actor/sequential_1/dense_4/Relu_1:activations:0:actor/sequential_1/dense_5/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes

: 2%
#actor/sequential_1/dense_5/MatMul_1á
3actor/sequential_1/dense_5/BiasAdd_1/ReadVariableOpReadVariableOp:actor_sequential_1_dense_5_biasadd_readvariableop_resource*
_output_shapes
:*
dtype025
3actor/sequential_1/dense_5/BiasAdd_1/ReadVariableOpì
$actor/sequential_1/dense_5/BiasAdd_1BiasAdd-actor/sequential_1/dense_5/MatMul_1:product:0;actor/sequential_1/dense_5/BiasAdd_1/ReadVariableOp:value:0*
T0*
_output_shapes

: 2&
$actor/sequential_1/dense_5/BiasAdd_1¦
!actor/sequential_1/dense_5/Tanh_1Tanh-actor/sequential_1/dense_5/BiasAdd_1:output:0*
T0*
_output_shapes

: 2#
!actor/sequential_1/dense_5/Tanh_1n
critic/concat_2/axisConst*
_output_shapes
: *
dtype0*
value	B :2
critic/concat_2/axisµ
critic/concat_2ConcatV2transitions_0%actor/sequential_1/dense_5/Tanh_1:y:0critic/concat_2/axis:output:0*
N*
T0*
_output_shapes

: 2
critic/concat_2Ú
/critic/sequential/dense/MatMul_2/ReadVariableOpReadVariableOp6critic_sequential_dense_matmul_readvariableop_resource*
_output_shapes
:	*
dtype021
/critic/sequential/dense/MatMul_2/ReadVariableOpË
 critic/sequential/dense/MatMul_2MatMulcritic/concat_2:output:07critic/sequential/dense/MatMul_2/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2"
 critic/sequential/dense/MatMul_2Ù
0critic/sequential/dense/BiasAdd_2/ReadVariableOpReadVariableOp7critic_sequential_dense_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype022
0critic/sequential/dense/BiasAdd_2/ReadVariableOpá
!critic/sequential/dense/BiasAdd_2BiasAdd*critic/sequential/dense/MatMul_2:product:08critic/sequential/dense/BiasAdd_2/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2#
!critic/sequential/dense/BiasAdd_2
critic/sequential/dense/Relu_2Relu*critic/sequential/dense/BiasAdd_2:output:0*
T0*
_output_shapes
:	 2 
critic/sequential/dense/Relu_2á
1critic/sequential/dense_1/MatMul_2/ReadVariableOpReadVariableOp8critic_sequential_dense_1_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype023
1critic/sequential/dense_1/MatMul_2/ReadVariableOpå
"critic/sequential/dense_1/MatMul_2MatMul,critic/sequential/dense/Relu_2:activations:09critic/sequential/dense_1/MatMul_2/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2$
"critic/sequential/dense_1/MatMul_2ß
2critic/sequential/dense_1/BiasAdd_2/ReadVariableOpReadVariableOp9critic_sequential_dense_1_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype024
2critic/sequential/dense_1/BiasAdd_2/ReadVariableOpé
#critic/sequential/dense_1/BiasAdd_2BiasAdd,critic/sequential/dense_1/MatMul_2:product:0:critic/sequential/dense_1/BiasAdd_2/ReadVariableOp:value:0*
T0*
_output_shapes
:	 2%
#critic/sequential/dense_1/BiasAdd_2¤
 critic/sequential/dense_1/Relu_2Relu,critic/sequential/dense_1/BiasAdd_2:output:0*
T0*
_output_shapes
:	 2"
 critic/sequential/dense_1/Relu_2à
1critic/sequential/dense_2/MatMul_2/ReadVariableOpReadVariableOp8critic_sequential_dense_2_matmul_readvariableop_resource*
_output_shapes
:	*
dtype023
1critic/sequential/dense_2/MatMul_2/ReadVariableOpæ
"critic/sequential/dense_2/MatMul_2MatMul.critic/sequential/dense_1/Relu_2:activations:09critic/sequential/dense_2/MatMul_2/ReadVariableOp:value:0*
T0*
_output_shapes

: 2$
"critic/sequential/dense_2/MatMul_2Þ
2critic/sequential/dense_2/BiasAdd_2/ReadVariableOpReadVariableOp9critic_sequential_dense_2_biasadd_readvariableop_resource*
_output_shapes
:*
dtype024
2critic/sequential/dense_2/BiasAdd_2/ReadVariableOpè
#critic/sequential/dense_2/BiasAdd_2BiasAdd,critic/sequential/dense_2/MatMul_2:product:0:critic/sequential/dense_2/BiasAdd_2/ReadVariableOp:value:0*
T0*
_output_shapes

: 2%
#critic/sequential/dense_2/BiasAdd_2h
NegNeg,critic/sequential/dense_2/BiasAdd_2:output:0*
T0*
_output_shapes

: 2
Neg_
ConstConst*
_output_shapes
:*
dtype0*
valueB"       2
ConstR
Mean_1MeanNeg:y:0Const:output:0*
T0*
_output_shapes
: 2
Mean_1Q
onesConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
ones
gradient_tape/Reshape/shapeConst*
_output_shapes
:*
dtype0*
valueB"      2
gradient_tape/Reshape/shape
gradient_tape/ReshapeReshapeones:output:0$gradient_tape/Reshape/shape:output:0*
T0*
_output_shapes

:2
gradient_tape/Reshape{
gradient_tape/ConstConst*
_output_shapes
:*
dtype0*
valueB"       2
gradient_tape/Const
gradient_tape/TileTilegradient_tape/Reshape:output:0gradient_tape/Const:output:0*
T0*
_output_shapes

: 2
gradient_tape/Tiles
gradient_tape/Const_1Const*
_output_shapes
: *
dtype0*
valueB
 *   B2
gradient_tape/Const_1
gradient_tape/truedivRealDivgradient_tape/Tile:output:0gradient_tape/Const_1:output:0*
T0*
_output_shapes

: 2
gradient_tape/truedivq
gradient_tape/NegNeggradient_tape/truediv:z:0*
T0*
_output_shapes

: 2
gradient_tape/NegÉ
=gradient_tape/critic/sequential/dense_2/BiasAdd_2/BiasAddGradBiasAddGradgradient_tape/Neg:y:0*
T0*
_output_shapes
:2?
=gradient_tape/critic/sequential/dense_2/BiasAdd_2/BiasAddGradù
.gradient_tape/critic/sequential/dense_2/MatMulMatMulgradient_tape/Neg:y:09critic/sequential/dense_2/MatMul_2/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(20
.gradient_tape/critic/sequential/dense_2/MatMulò
0gradient_tape/critic/sequential/dense_2/MatMul_1MatMul.critic/sequential/dense_1/Relu_2:activations:0gradient_tape/Neg:y:0*
T0*
_output_shapes
:	*
transpose_a(22
0gradient_tape/critic/sequential/dense_2/MatMul_1
0gradient_tape/critic/sequential/dense_1/ReluGradReluGrad8gradient_tape/critic/sequential/dense_2/MatMul:product:0.critic/sequential/dense_1/Relu_2:activations:0*
T0*
_output_shapes
:	 22
0gradient_tape/critic/sequential/dense_1/ReluGradñ
=gradient_tape/critic/sequential/dense_1/BiasAdd_2/BiasAddGradBiasAddGrad<gradient_tape/critic/sequential/dense_1/ReluGrad:backprops:0*
T0*
_output_shapes	
:2?
=gradient_tape/critic/sequential/dense_1/BiasAdd_2/BiasAddGrad 
.gradient_tape/critic/sequential/dense_1/MatMulMatMul<gradient_tape/critic/sequential/dense_1/ReluGrad:backprops:09critic/sequential/dense_1/MatMul_2/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(20
.gradient_tape/critic/sequential/dense_1/MatMul
0gradient_tape/critic/sequential/dense_1/MatMul_1MatMul,critic/sequential/dense/Relu_2:activations:0<gradient_tape/critic/sequential/dense_1/ReluGrad:backprops:0*
T0* 
_output_shapes
:
*
transpose_a(22
0gradient_tape/critic/sequential/dense_1/MatMul_1þ
.gradient_tape/critic/sequential/dense/ReluGradReluGrad8gradient_tape/critic/sequential/dense_1/MatMul:product:0,critic/sequential/dense/Relu_2:activations:0*
T0*
_output_shapes
:	 20
.gradient_tape/critic/sequential/dense/ReluGradë
;gradient_tape/critic/sequential/dense/BiasAdd_2/BiasAddGradBiasAddGrad:gradient_tape/critic/sequential/dense/ReluGrad:backprops:0*
T0*
_output_shapes	
:2=
;gradient_tape/critic/sequential/dense/BiasAdd_2/BiasAddGrad
,gradient_tape/critic/sequential/dense/MatMulMatMul:gradient_tape/critic/sequential/dense/ReluGrad:backprops:07critic/sequential/dense/MatMul_2/ReadVariableOp:value:0*
T0*
_output_shapes

: *
transpose_b(2.
,gradient_tape/critic/sequential/dense/MatMulý
.gradient_tape/critic/sequential/dense/MatMul_1MatMulcritic/concat_2:output:0:gradient_tape/critic/sequential/dense/ReluGrad:backprops:0*
T0*
_output_shapes
:	*
transpose_a(20
.gradient_tape/critic/sequential/dense/MatMul_1x
gradient_tape/critic/RankConst*
_output_shapes
: *
dtype0*
value	B :2
gradient_tape/critic/Rank¤
gradient_tape/critic/modFloorModcritic/concat_2/axis:output:0"gradient_tape/critic/Rank:output:0*
T0*
_output_shapes
: 2
gradient_tape/critic/mod
gradient_tape/critic/ShapeConst*
_output_shapes
:*
dtype0*
valueB"       2
gradient_tape/critic/Shape
gradient_tape/critic/Shape_1Const*
_output_shapes
:*
dtype0*
valueB"       2
gradient_tape/critic/Shape_1ë
!gradient_tape/critic/ConcatOffsetConcatOffsetgradient_tape/critic/mod:z:0#gradient_tape/critic/Shape:output:0%gradient_tape/critic/Shape_1:output:0*
N* 
_output_shapes
::2#
!gradient_tape/critic/ConcatOffset
gradient_tape/critic/SliceSlice6gradient_tape/critic/sequential/dense/MatMul:product:0*gradient_tape/critic/ConcatOffset:offset:0#gradient_tape/critic/Shape:output:0*
Index0*
T0*
_output_shapes

: 2
gradient_tape/critic/Slice
gradient_tape/critic/Slice_1Slice6gradient_tape/critic/sequential/dense/MatMul:product:0*gradient_tape/critic/ConcatOffset:offset:1%gradient_tape/critic/Shape_1:output:0*
Index0*
T0*
_output_shapes

: 2
gradient_tape/critic/Slice_1é
1gradient_tape/actor/sequential_1/dense_5/TanhGradTanhGrad%actor/sequential_1/dense_5/Tanh_1:y:0%gradient_tape/critic/Slice_1:output:0*
T0*
_output_shapes

: 23
1gradient_tape/actor/sequential_1/dense_5/TanhGradë
>gradient_tape/actor/sequential_1/dense_5/BiasAdd_1/BiasAddGradBiasAddGrad5gradient_tape/actor/sequential_1/dense_5/TanhGrad:z:0*
T0*
_output_shapes
:2@
>gradient_tape/actor/sequential_1/dense_5/BiasAdd_1/BiasAddGrad
/gradient_tape/actor/sequential_1/dense_5/MatMulMatMul5gradient_tape/actor/sequential_1/dense_5/TanhGrad:z:0:actor/sequential_1/dense_5/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(21
/gradient_tape/actor/sequential_1/dense_5/MatMul
1gradient_tape/actor/sequential_1/dense_5/MatMul_1MatMul/actor/sequential_1/dense_4/Relu_1:activations:05gradient_tape/actor/sequential_1/dense_5/TanhGrad:z:0*
T0*
_output_shapes
:	*
transpose_a(23
1gradient_tape/actor/sequential_1/dense_5/MatMul_1
1gradient_tape/actor/sequential_1/dense_4/ReluGradReluGrad9gradient_tape/actor/sequential_1/dense_5/MatMul:product:0/actor/sequential_1/dense_4/Relu_1:activations:0*
T0*
_output_shapes
:	 23
1gradient_tape/actor/sequential_1/dense_4/ReluGradô
>gradient_tape/actor/sequential_1/dense_4/BiasAdd_1/BiasAddGradBiasAddGrad=gradient_tape/actor/sequential_1/dense_4/ReluGrad:backprops:0*
T0*
_output_shapes	
:2@
>gradient_tape/actor/sequential_1/dense_4/BiasAdd_1/BiasAddGrad¤
/gradient_tape/actor/sequential_1/dense_4/MatMulMatMul=gradient_tape/actor/sequential_1/dense_4/ReluGrad:backprops:0:actor/sequential_1/dense_4/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(21
/gradient_tape/actor/sequential_1/dense_4/MatMul
1gradient_tape/actor/sequential_1/dense_4/MatMul_1MatMul/actor/sequential_1/dense_3/Relu_1:activations:0=gradient_tape/actor/sequential_1/dense_4/ReluGrad:backprops:0*
T0* 
_output_shapes
:
*
transpose_a(23
1gradient_tape/actor/sequential_1/dense_4/MatMul_1
1gradient_tape/actor/sequential_1/dense_3/ReluGradReluGrad9gradient_tape/actor/sequential_1/dense_4/MatMul:product:0/actor/sequential_1/dense_3/Relu_1:activations:0*
T0*
_output_shapes
:	 23
1gradient_tape/actor/sequential_1/dense_3/ReluGradô
>gradient_tape/actor/sequential_1/dense_3/BiasAdd_1/BiasAddGradBiasAddGrad=gradient_tape/actor/sequential_1/dense_3/ReluGrad:backprops:0*
T0*
_output_shapes	
:2@
>gradient_tape/actor/sequential_1/dense_3/BiasAdd_1/BiasAddGrad÷
/gradient_tape/actor/sequential_1/dense_3/MatMulMatMultransitions_0=gradient_tape/actor/sequential_1/dense_3/ReluGrad:backprops:0*
T0*
_output_shapes
:	*
transpose_a(21
/gradient_tape/actor/sequential_1/dense_3/MatMulÒ
global_norm/L2LossL2Loss9gradient_tape/actor/sequential_1/dense_3/MatMul:product:0*
T0*B
_class8
64loc:@gradient_tape/actor/sequential_1/dense_3/MatMul*
_output_shapes
: 2
global_norm/L2Lossó
global_norm/L2Loss_1L2LossGgradient_tape/actor/sequential_1/dense_3/BiasAdd_1/BiasAddGrad:output:0*
T0*Q
_classG
ECloc:@gradient_tape/actor/sequential_1/dense_3/BiasAdd_1/BiasAddGrad*
_output_shapes
: 2
global_norm/L2Loss_1Ú
global_norm/L2Loss_2L2Loss;gradient_tape/actor/sequential_1/dense_4/MatMul_1:product:0*
T0*D
_class:
86loc:@gradient_tape/actor/sequential_1/dense_4/MatMul_1*
_output_shapes
: 2
global_norm/L2Loss_2ó
global_norm/L2Loss_3L2LossGgradient_tape/actor/sequential_1/dense_4/BiasAdd_1/BiasAddGrad:output:0*
T0*Q
_classG
ECloc:@gradient_tape/actor/sequential_1/dense_4/BiasAdd_1/BiasAddGrad*
_output_shapes
: 2
global_norm/L2Loss_3Ú
global_norm/L2Loss_4L2Loss;gradient_tape/actor/sequential_1/dense_5/MatMul_1:product:0*
T0*D
_class:
86loc:@gradient_tape/actor/sequential_1/dense_5/MatMul_1*
_output_shapes
: 2
global_norm/L2Loss_4ó
global_norm/L2Loss_5L2LossGgradient_tape/actor/sequential_1/dense_5/BiasAdd_1/BiasAddGrad:output:0*
T0*Q
_classG
ECloc:@gradient_tape/actor/sequential_1/dense_5/BiasAdd_1/BiasAddGrad*
_output_shapes
: 2
global_norm/L2Loss_5
global_norm/stackPackglobal_norm/L2Loss:output:0global_norm/L2Loss_1:output:0global_norm/L2Loss_2:output:0global_norm/L2Loss_3:output:0global_norm/L2Loss_4:output:0global_norm/L2Loss_5:output:0*
N*
T0*
_output_shapes
:2
global_norm/stackp
global_norm/ConstConst*
_output_shapes
:*
dtype0*
valueB: 2
global_norm/Const
global_norm/SumSumglobal_norm/stack:output:0global_norm/Const:output:0*
T0*
_output_shapes
: 2
global_norm/Sumo
global_norm/Const_1Const*
_output_shapes
: *
dtype0*
valueB
 *   @2
global_norm/Const_1
global_norm/mulMulglobal_norm/Sum:output:0global_norm/Const_1:output:0*
T0*
_output_shapes
: 2
global_norm/mulp
global_norm/global_normSqrtglobal_norm/mul:z:0*
T0*
_output_shapes
: 2
global_norm/global_norm
clip_by_global_norm/truediv/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
clip_by_global_norm/truediv/x«
clip_by_global_norm/truedivRealDiv&clip_by_global_norm/truediv/x:output:0global_norm/global_norm:y:0*
T0*
_output_shapes
: 2
clip_by_global_norm/truediv{
clip_by_global_norm/ConstConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
clip_by_global_norm/Const
clip_by_global_norm/truediv_1/yConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2!
clip_by_global_norm/truediv_1/y¸
clip_by_global_norm/truediv_1RealDiv"clip_by_global_norm/Const:output:0(clip_by_global_norm/truediv_1/y:output:0*
T0*
_output_shapes
: 2
clip_by_global_norm/truediv_1ª
clip_by_global_norm/MinimumMinimumclip_by_global_norm/truediv:z:0!clip_by_global_norm/truediv_1:z:0*
T0*
_output_shapes
: 2
clip_by_global_norm/Minimum{
clip_by_global_norm/mul/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
clip_by_global_norm/mul/x
clip_by_global_norm/mulMul"clip_by_global_norm/mul/x:output:0clip_by_global_norm/Minimum:z:0*
T0*
_output_shapes
: 2
clip_by_global_norm/mul
clip_by_global_norm/subSubglobal_norm/global_norm:y:0global_norm/global_norm:y:0*
T0*
_output_shapes
: 2
clip_by_global_norm/sub
clip_by_global_norm/addAddV2clip_by_global_norm/mul:z:0clip_by_global_norm/sub:z:0*
T0*
_output_shapes
: 2
clip_by_global_norm/add
clip_by_global_norm/mul_1Mul9gradient_tape/actor/sequential_1/dense_3/MatMul:product:0clip_by_global_norm/add:z:0*
T0*B
_class8
64loc:@gradient_tape/actor/sequential_1/dense_3/MatMul*
_output_shapes
:	2
clip_by_global_norm/mul_1ñ
*clip_by_global_norm/clip_by_global_norm/_0Identityclip_by_global_norm/mul_1:z:0*
T0*B
_class8
64loc:@gradient_tape/actor/sequential_1/dense_3/MatMul*
_output_shapes
:	2,
*clip_by_global_norm/clip_by_global_norm/_0
clip_by_global_norm/mul_2MulGgradient_tape/actor/sequential_1/dense_3/BiasAdd_1/BiasAddGrad:output:0clip_by_global_norm/add:z:0*
T0*Q
_classG
ECloc:@gradient_tape/actor/sequential_1/dense_3/BiasAdd_1/BiasAddGrad*
_output_shapes	
:2
clip_by_global_norm/mul_2ü
*clip_by_global_norm/clip_by_global_norm/_1Identityclip_by_global_norm/mul_2:z:0*
T0*Q
_classG
ECloc:@gradient_tape/actor/sequential_1/dense_3/BiasAdd_1/BiasAddGrad*
_output_shapes	
:2,
*clip_by_global_norm/clip_by_global_norm/_1
clip_by_global_norm/mul_3Mul;gradient_tape/actor/sequential_1/dense_4/MatMul_1:product:0clip_by_global_norm/add:z:0*
T0*D
_class:
86loc:@gradient_tape/actor/sequential_1/dense_4/MatMul_1* 
_output_shapes
:
2
clip_by_global_norm/mul_3ô
*clip_by_global_norm/clip_by_global_norm/_2Identityclip_by_global_norm/mul_3:z:0*
T0*D
_class:
86loc:@gradient_tape/actor/sequential_1/dense_4/MatMul_1* 
_output_shapes
:
2,
*clip_by_global_norm/clip_by_global_norm/_2
clip_by_global_norm/mul_4MulGgradient_tape/actor/sequential_1/dense_4/BiasAdd_1/BiasAddGrad:output:0clip_by_global_norm/add:z:0*
T0*Q
_classG
ECloc:@gradient_tape/actor/sequential_1/dense_4/BiasAdd_1/BiasAddGrad*
_output_shapes	
:2
clip_by_global_norm/mul_4ü
*clip_by_global_norm/clip_by_global_norm/_3Identityclip_by_global_norm/mul_4:z:0*
T0*Q
_classG
ECloc:@gradient_tape/actor/sequential_1/dense_4/BiasAdd_1/BiasAddGrad*
_output_shapes	
:2,
*clip_by_global_norm/clip_by_global_norm/_3
clip_by_global_norm/mul_5Mul;gradient_tape/actor/sequential_1/dense_5/MatMul_1:product:0clip_by_global_norm/add:z:0*
T0*D
_class:
86loc:@gradient_tape/actor/sequential_1/dense_5/MatMul_1*
_output_shapes
:	2
clip_by_global_norm/mul_5ó
*clip_by_global_norm/clip_by_global_norm/_4Identityclip_by_global_norm/mul_5:z:0*
T0*D
_class:
86loc:@gradient_tape/actor/sequential_1/dense_5/MatMul_1*
_output_shapes
:	2,
*clip_by_global_norm/clip_by_global_norm/_4
clip_by_global_norm/mul_6MulGgradient_tape/actor/sequential_1/dense_5/BiasAdd_1/BiasAddGrad:output:0clip_by_global_norm/add:z:0*
T0*Q
_classG
ECloc:@gradient_tape/actor/sequential_1/dense_5/BiasAdd_1/BiasAddGrad*
_output_shapes
:2
clip_by_global_norm/mul_6û
*clip_by_global_norm/clip_by_global_norm/_5Identityclip_by_global_norm/mul_6:z:0*
T0*Q
_classG
ECloc:@gradient_tape/actor/sequential_1/dense_5/BiasAdd_1/BiasAddGrad*
_output_shapes
:2,
*clip_by_global_norm/clip_by_global_norm/_5U
ones_1Const*
_output_shapes
: *
dtype0*
valueB
 *  ?2
ones_1v
gradient_tape/Cast/xConst*
_output_shapes
:*
dtype0*
valueB: 2
gradient_tape/Cast/x
gradient_tape/Cast_1/xConst*
_output_shapes
:*
dtype0*
valueB:
ÿÿÿÿÿÿÿÿÿ2
gradient_tape/Cast_1/xj
gradient_tape/SizeConst*
_output_shapes
: *
dtype0*
value	B :2
gradient_tape/Size
gradient_tape/addAddV2gradient_tape/Cast_1/x:output:0gradient_tape/Size:output:0*
T0*
_output_shapes
:2
gradient_tape/add
gradient_tape/modFloorModgradient_tape/add:z:0gradient_tape/Size:output:0*
T0*
_output_shapes
:2
gradient_tape/modt
gradient_tape/ShapeConst*
_output_shapes
:*
dtype0*
valueB:2
gradient_tape/Shapex
gradient_tape/range/startConst*
_output_shapes
: *
dtype0*
value	B : 2
gradient_tape/range/startx
gradient_tape/range/deltaConst*
_output_shapes
: *
dtype0*
value	B :2
gradient_tape/range/delta´
gradient_tape/rangeRange"gradient_tape/range/start:output:0gradient_tape/Size:output:0"gradient_tape/range/delta:output:0*
_output_shapes
:2
gradient_tape/rangev
gradient_tape/Fill/valueConst*
_output_shapes
: *
dtype0*
value	B :2
gradient_tape/Fill/value
gradient_tape/FillFillgradient_tape/Shape:output:0!gradient_tape/Fill/value:output:0*
T0*
_output_shapes
:2
gradient_tape/Fillê
gradient_tape/DynamicStitchDynamicStitchgradient_tape/range:output:0gradient_tape/mod:z:0gradient_tape/Cast/x:output:0gradient_tape/Fill:output:0*
N*
T0*
_output_shapes
:2
gradient_tape/DynamicStitch|
gradient_tape/Maximum/xConst*
_output_shapes
:*
dtype0*
valueB:2
gradient_tape/Maximum/xt
gradient_tape/Maximum/yConst*
_output_shapes
: *
dtype0*
value	B :2
gradient_tape/Maximum/y¢
gradient_tape/MaximumMaximum gradient_tape/Maximum/x:output:0 gradient_tape/Maximum/y:output:0*
T0*
_output_shapes
:2
gradient_tape/Maximum~
gradient_tape/floordiv/xConst*
_output_shapes
:*
dtype0*
valueB: 2
gradient_tape/floordiv/x
gradient_tape/floordivFloorDiv!gradient_tape/floordiv/x:output:0gradient_tape/Maximum:z:0*
T0*
_output_shapes
:2
gradient_tape/floordiv
gradient_tape/Reshape_1/shapeConst*
_output_shapes
:*
dtype0*
valueB:2
gradient_tape/Reshape_1/shape
gradient_tape/Reshape_1Reshapeones_1:output:0&gradient_tape/Reshape_1/shape:output:0*
T0*
_output_shapes
:2
gradient_tape/Reshape_1
gradient_tape/Tile_1/multiplesConst*
_output_shapes
:*
dtype0*
valueB: 2 
gradient_tape/Tile_1/multiples¤
gradient_tape/Tile_1Tile gradient_tape/Reshape_1:output:0'gradient_tape/Tile_1/multiples:output:0*
T0*
_output_shapes
: 2
gradient_tape/Tile_1s
gradient_tape/Const_2Const*
_output_shapes
: *
dtype0*
valueB
 *   B2
gradient_tape/Const_2¡
gradient_tape/truediv_1RealDivgradient_tape/Tile_1:output:0gradient_tape/Const_2:output:0*
T0*
_output_shapes
: 2
gradient_tape/truediv_1
gradient_tape/scalarConst^gradient_tape/truediv_1*
_output_shapes
: *
dtype0*
valueB
 *   @2
gradient_tape/scalar
gradient_tape/MulMulgradient_tape/scalar:output:0gradient_tape/truediv_1:z:0*
T0*
_output_shapes
: 2
gradient_tape/Mul
gradient_tape/subSubSqueeze_1:output:0add:z:0^gradient_tape/truediv_1*
T0*
_output_shapes
: 2
gradient_tape/sub
gradient_tape/mul_1Mulgradient_tape/Mul:z:0gradient_tape/sub:z:0*
T0*
_output_shapes
: 2
gradient_tape/mul_1o
gradient_tape/Neg_1Neggradient_tape/mul_1:z:0*
T0*
_output_shapes
: 2
gradient_tape/Neg_1
gradient_tape/Shape_1Const*
_output_shapes
:*
dtype0*
valueB"       2
gradient_tape/Shape_1
gradient_tape/Reshape_2Reshapegradient_tape/mul_1:z:0gradient_tape/Shape_1:output:0*
T0*
_output_shapes

: 2
gradient_tape/Reshape_2Ô
=gradient_tape/critic/sequential/dense_2/BiasAdd_1/BiasAddGradBiasAddGrad gradient_tape/Reshape_2:output:0*
T0*
_output_shapes
:2?
=gradient_tape/critic/sequential/dense_2/BiasAdd_1/BiasAddGrad
gradient_tape/mul_1/MulMulgradient_tape/Neg_1:y:0sub:z:0*
T0*
_output_shapes
: 2
gradient_tape/mul_1/Mul
gradient_tape/mul_1/Mul_1Mulgradient_tape/Neg_1:y:0mul:z:0*
T0*
_output_shapes
: 2
gradient_tape/mul_1/Mul_1
0gradient_tape/critic/sequential/dense_2/MatMul_2MatMul gradient_tape/Reshape_2:output:09critic/sequential/dense_2/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(22
0gradient_tape/critic/sequential/dense_2/MatMul_2ý
0gradient_tape/critic/sequential/dense_2/MatMul_3MatMul.critic/sequential/dense_1/Relu_1:activations:0 gradient_tape/Reshape_2:output:0*
T0*
_output_shapes
:	*
transpose_a(22
0gradient_tape/critic/sequential/dense_2/MatMul_3
*gradient_tape/mul/BroadcastGradientArgs/s0Const*
_output_shapes
: *
dtype0*
valueB 2,
*gradient_tape/mul/BroadcastGradientArgs/s0¢
*gradient_tape/mul/BroadcastGradientArgs/s1Const*
_output_shapes
:*
dtype0*
valueB: 2,
*gradient_tape/mul/BroadcastGradientArgs/s1
'gradient_tape/mul/BroadcastGradientArgsBroadcastGradientArgs3gradient_tape/mul/BroadcastGradientArgs/s0:output:03gradient_tape/mul/BroadcastGradientArgs/s1:output:0*2
_output_shapes 
:ÿÿÿÿÿÿÿÿÿ:ÿÿÿÿÿÿÿÿÿ2)
'gradient_tape/mul/BroadcastGradientArgs
gradient_tape/mul/MulMulmul/x:output:0gradient_tape/mul_1/Mul:z:0*
T0*
_output_shapes
: 2
gradient_tape/mul/Mul
2gradient_tape/critic/sequential/dense_1/ReluGrad_1ReluGrad:gradient_tape/critic/sequential/dense_2/MatMul_2:product:0.critic/sequential/dense_1/Relu_1:activations:0*
T0*
_output_shapes
:	 24
2gradient_tape/critic/sequential/dense_1/ReluGrad_1
gradient_tape/Shape_2Const*
_output_shapes
:*
dtype0*
valueB"       2
gradient_tape/Shape_2¡
gradient_tape/Reshape_3Reshapegradient_tape/mul/Mul:z:0gradient_tape/Shape_2:output:0*
T0*
_output_shapes

: 2
gradient_tape/Reshape_3ó
=gradient_tape/critic/sequential/dense_1/BiasAdd_1/BiasAddGradBiasAddGrad>gradient_tape/critic/sequential/dense_1/ReluGrad_1:backprops:0*
T0*
_output_shapes	
:2?
=gradient_tape/critic/sequential/dense_1/BiasAdd_1/BiasAddGradÐ
;gradient_tape/critic/sequential/dense_2/BiasAdd/BiasAddGradBiasAddGrad gradient_tape/Reshape_3:output:0*
T0*
_output_shapes
:2=
;gradient_tape/critic/sequential/dense_2/BiasAdd/BiasAddGrad¦
0gradient_tape/critic/sequential/dense_1/MatMul_2MatMul>gradient_tape/critic/sequential/dense_1/ReluGrad_1:backprops:09critic/sequential/dense_1/MatMul_1/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(22
0gradient_tape/critic/sequential/dense_1/MatMul_2
0gradient_tape/critic/sequential/dense_1/MatMul_3MatMul,critic/sequential/dense/Relu_1:activations:0>gradient_tape/critic/sequential/dense_1/ReluGrad_1:backprops:0*
T0* 
_output_shapes
:
*
transpose_a(22
0gradient_tape/critic/sequential/dense_1/MatMul_3
0gradient_tape/critic/sequential/dense_2/MatMul_4MatMul gradient_tape/Reshape_3:output:07critic/sequential/dense_2/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(22
0gradient_tape/critic/sequential/dense_2/MatMul_4û
0gradient_tape/critic/sequential/dense_2/MatMul_5MatMul,critic/sequential/dense_1/Relu:activations:0 gradient_tape/Reshape_3:output:0*
T0*
_output_shapes
:	*
transpose_a(22
0gradient_tape/critic/sequential/dense_2/MatMul_5
0gradient_tape/critic/sequential/dense/ReluGrad_1ReluGrad:gradient_tape/critic/sequential/dense_1/MatMul_2:product:0,critic/sequential/dense/Relu_1:activations:0*
T0*
_output_shapes
:	 22
0gradient_tape/critic/sequential/dense/ReluGrad_1
2gradient_tape/critic/sequential/dense_1/ReluGrad_2ReluGrad:gradient_tape/critic/sequential/dense_2/MatMul_4:product:0,critic/sequential/dense_1/Relu:activations:0*
T0*
_output_shapes
:	 24
2gradient_tape/critic/sequential/dense_1/ReluGrad_2í
;gradient_tape/critic/sequential/dense/BiasAdd_1/BiasAddGradBiasAddGrad<gradient_tape/critic/sequential/dense/ReluGrad_1:backprops:0*
T0*
_output_shapes	
:2=
;gradient_tape/critic/sequential/dense/BiasAdd_1/BiasAddGradï
;gradient_tape/critic/sequential/dense_1/BiasAdd/BiasAddGradBiasAddGrad>gradient_tape/critic/sequential/dense_1/ReluGrad_2:backprops:0*
T0*
_output_shapes	
:2=
;gradient_tape/critic/sequential/dense_1/BiasAdd/BiasAddGradÿ
.gradient_tape/critic/sequential/dense/MatMul_2MatMulcritic/concat_1:output:0<gradient_tape/critic/sequential/dense/ReluGrad_1:backprops:0*
T0*
_output_shapes
:	*
transpose_a(20
.gradient_tape/critic/sequential/dense/MatMul_2¤
0gradient_tape/critic/sequential/dense_1/MatMul_4MatMul>gradient_tape/critic/sequential/dense_1/ReluGrad_2:backprops:07critic/sequential/dense_1/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(22
0gradient_tape/critic/sequential/dense_1/MatMul_4
0gradient_tape/critic/sequential/dense_1/MatMul_5MatMul*critic/sequential/dense/Relu:activations:0>gradient_tape/critic/sequential/dense_1/ReluGrad_2:backprops:0*
T0* 
_output_shapes
:
*
transpose_a(22
0gradient_tape/critic/sequential/dense_1/MatMul_5
0gradient_tape/critic/sequential/dense/ReluGrad_2ReluGrad:gradient_tape/critic/sequential/dense_1/MatMul_4:product:0*critic/sequential/dense/Relu:activations:0*
T0*
_output_shapes
:	 22
0gradient_tape/critic/sequential/dense/ReluGrad_2é
9gradient_tape/critic/sequential/dense/BiasAdd/BiasAddGradBiasAddGrad<gradient_tape/critic/sequential/dense/ReluGrad_2:backprops:0*
T0*
_output_shapes	
:2;
9gradient_tape/critic/sequential/dense/BiasAdd/BiasAddGrad
.gradient_tape/critic/sequential/dense/MatMul_3MatMul<gradient_tape/critic/sequential/dense/ReluGrad_2:backprops:05critic/sequential/dense/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes

: *
transpose_b(20
.gradient_tape/critic/sequential/dense/MatMul_3ý
.gradient_tape/critic/sequential/dense/MatMul_4MatMulcritic/concat:output:0<gradient_tape/critic/sequential/dense/ReluGrad_2:backprops:0*
T0*
_output_shapes
:	*
transpose_a(20
.gradient_tape/critic/sequential/dense/MatMul_4|
gradient_tape/critic/Rank_1Const*
_output_shapes
: *
dtype0*
value	B :2
gradient_tape/critic/Rank_1¨
gradient_tape/critic/mod_1FloorModcritic/concat/axis:output:0$gradient_tape/critic/Rank_1:output:0*
T0*
_output_shapes
: 2
gradient_tape/critic/mod_1
gradient_tape/critic/Shape_2Const*
_output_shapes
:*
dtype0*
valueB"       2
gradient_tape/critic/Shape_2
gradient_tape/critic/Shape_3Const*
_output_shapes
:*
dtype0*
valueB"       2
gradient_tape/critic/Shape_3ó
#gradient_tape/critic/ConcatOffset_1ConcatOffsetgradient_tape/critic/mod_1:z:0%gradient_tape/critic/Shape_2:output:0%gradient_tape/critic/Shape_3:output:0*
N* 
_output_shapes
::2%
#gradient_tape/critic/ConcatOffset_1
gradient_tape/critic/Slice_2Slice8gradient_tape/critic/sequential/dense/MatMul_3:product:0,gradient_tape/critic/ConcatOffset_1:offset:0%gradient_tape/critic/Shape_2:output:0*
Index0*
T0*
_output_shapes

: 2
gradient_tape/critic/Slice_2
gradient_tape/critic/Slice_3Slice8gradient_tape/critic/sequential/dense/MatMul_3:product:0,gradient_tape/critic/ConcatOffset_1:offset:1%gradient_tape/critic/Shape_3:output:0*
Index0*
T0*
_output_shapes

: 2
gradient_tape/critic/Slice_3ë
3gradient_tape/actor/sequential_1/dense_5/TanhGrad_1TanhGrad#actor/sequential_1/dense_5/Tanh:y:0%gradient_tape/critic/Slice_3:output:0*
T0*
_output_shapes

: 25
3gradient_tape/actor/sequential_1/dense_5/TanhGrad_1é
<gradient_tape/actor/sequential_1/dense_5/BiasAdd/BiasAddGradBiasAddGrad7gradient_tape/actor/sequential_1/dense_5/TanhGrad_1:z:0*
T0*
_output_shapes
:2>
<gradient_tape/actor/sequential_1/dense_5/BiasAdd/BiasAddGrad 
1gradient_tape/actor/sequential_1/dense_5/MatMul_2MatMul7gradient_tape/actor/sequential_1/dense_5/TanhGrad_1:z:08actor/sequential_1/dense_5/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(23
1gradient_tape/actor/sequential_1/dense_5/MatMul_2
1gradient_tape/actor/sequential_1/dense_5/MatMul_3MatMul-actor/sequential_1/dense_4/Relu:activations:07gradient_tape/actor/sequential_1/dense_5/TanhGrad_1:z:0*
T0*
_output_shapes
:	*
transpose_a(23
1gradient_tape/actor/sequential_1/dense_5/MatMul_3
3gradient_tape/actor/sequential_1/dense_4/ReluGrad_1ReluGrad;gradient_tape/actor/sequential_1/dense_5/MatMul_2:product:0-actor/sequential_1/dense_4/Relu:activations:0*
T0*
_output_shapes
:	 25
3gradient_tape/actor/sequential_1/dense_4/ReluGrad_1ò
<gradient_tape/actor/sequential_1/dense_4/BiasAdd/BiasAddGradBiasAddGrad?gradient_tape/actor/sequential_1/dense_4/ReluGrad_1:backprops:0*
T0*
_output_shapes	
:2>
<gradient_tape/actor/sequential_1/dense_4/BiasAdd/BiasAddGrad¨
1gradient_tape/actor/sequential_1/dense_4/MatMul_2MatMul?gradient_tape/actor/sequential_1/dense_4/ReluGrad_1:backprops:08actor/sequential_1/dense_4/MatMul/ReadVariableOp:value:0*
T0*
_output_shapes
:	 *
transpose_b(23
1gradient_tape/actor/sequential_1/dense_4/MatMul_2
1gradient_tape/actor/sequential_1/dense_4/MatMul_3MatMul-actor/sequential_1/dense_3/Relu:activations:0?gradient_tape/actor/sequential_1/dense_4/ReluGrad_1:backprops:0*
T0* 
_output_shapes
:
*
transpose_a(23
1gradient_tape/actor/sequential_1/dense_4/MatMul_3
3gradient_tape/actor/sequential_1/dense_3/ReluGrad_1ReluGrad;gradient_tape/actor/sequential_1/dense_4/MatMul_2:product:0-actor/sequential_1/dense_3/Relu:activations:0*
T0*
_output_shapes
:	 25
3gradient_tape/actor/sequential_1/dense_3/ReluGrad_1ò
<gradient_tape/actor/sequential_1/dense_3/BiasAdd/BiasAddGradBiasAddGrad?gradient_tape/actor/sequential_1/dense_3/ReluGrad_1:backprops:0*
T0*
_output_shapes	
:2>
<gradient_tape/actor/sequential_1/dense_3/BiasAdd/BiasAddGradý
1gradient_tape/actor/sequential_1/dense_3/MatMul_1MatMultransitions_2?gradient_tape/actor/sequential_1/dense_3/ReluGrad_1:backprops:0*
T0*
_output_shapes
:	*
transpose_a(23
1gradient_tape/actor/sequential_1/dense_3/MatMul_1»
AddNAddN8gradient_tape/critic/sequential/dense/MatMul_2:product:08gradient_tape/critic/sequential/dense/MatMul_4:product:0*
N*
T0*
_output_shapes
:	2
AddNÑ
AddN_1AddNDgradient_tape/critic/sequential/dense/BiasAdd_1/BiasAddGrad:output:0Bgradient_tape/critic/sequential/dense/BiasAdd/BiasAddGrad:output:0*
N*
T0*
_output_shapes	
:2
AddN_1Ä
AddN_2AddN:gradient_tape/critic/sequential/dense_1/MatMul_3:product:0:gradient_tape/critic/sequential/dense_1/MatMul_5:product:0*
N*
T0* 
_output_shapes
:
2
AddN_2Õ
AddN_3AddNFgradient_tape/critic/sequential/dense_1/BiasAdd_1/BiasAddGrad:output:0Dgradient_tape/critic/sequential/dense_1/BiasAdd/BiasAddGrad:output:0*
N*
T0*
_output_shapes	
:2
AddN_3Ã
AddN_4AddN:gradient_tape/critic/sequential/dense_2/MatMul_3:product:0:gradient_tape/critic/sequential/dense_2/MatMul_5:product:0*
N*
T0*
_output_shapes
:	2
AddN_4Ô
AddN_5AddNFgradient_tape/critic/sequential/dense_2/BiasAdd_1/BiasAddGrad:output:0Dgradient_tape/critic/sequential/dense_2/BiasAdd/BiasAddGrad:output:0*
N*
T0*
_output_shapes
:2
AddN_5|
global_norm_1/L2LossL2Loss
AddN:sum:0*
T0*
_class
	loc:@AddN*
_output_shapes
: 2
global_norm_1/L2Loss
global_norm_1/L2Loss_1L2LossAddN_1:sum:0*
T0*
_class
loc:@AddN_1*
_output_shapes
: 2
global_norm_1/L2Loss_1
global_norm_1/L2Loss_2L2LossAddN_2:sum:0*
T0*
_class
loc:@AddN_2*
_output_shapes
: 2
global_norm_1/L2Loss_2
global_norm_1/L2Loss_3L2LossAddN_3:sum:0*
T0*
_class
loc:@AddN_3*
_output_shapes
: 2
global_norm_1/L2Loss_3
global_norm_1/L2Loss_4L2LossAddN_4:sum:0*
T0*
_class
loc:@AddN_4*
_output_shapes
: 2
global_norm_1/L2Loss_4
global_norm_1/L2Loss_5L2LossAddN_5:sum:0*
T0*
_class
loc:@AddN_5*
_output_shapes
: 2
global_norm_1/L2Loss_5¤
global_norm_1/stackPackglobal_norm_1/L2Loss:output:0global_norm_1/L2Loss_1:output:0global_norm_1/L2Loss_2:output:0global_norm_1/L2Loss_3:output:0global_norm_1/L2Loss_4:output:0global_norm_1/L2Loss_5:output:0*
N*
T0*
_output_shapes
:2
global_norm_1/stackt
global_norm_1/ConstConst*
_output_shapes
:*
dtype0*
valueB: 2
global_norm_1/Const
global_norm_1/SumSumglobal_norm_1/stack:output:0global_norm_1/Const:output:0*
T0*
_output_shapes
: 2
global_norm_1/Sums
global_norm_1/Const_1Const*
_output_shapes
: *
dtype0*
valueB
 *   @2
global_norm_1/Const_1
global_norm_1/mulMulglobal_norm_1/Sum:output:0global_norm_1/Const_1:output:0*
T0*
_output_shapes
: 2
global_norm_1/mulv
global_norm_1/global_normSqrtglobal_norm_1/mul:z:0*
T0*
_output_shapes
: 2
global_norm_1/global_norm
clip_by_global_norm_1/truediv/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2!
clip_by_global_norm_1/truediv/x³
clip_by_global_norm_1/truedivRealDiv(clip_by_global_norm_1/truediv/x:output:0global_norm_1/global_norm:y:0*
T0*
_output_shapes
: 2
clip_by_global_norm_1/truediv
clip_by_global_norm_1/ConstConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
clip_by_global_norm_1/Const
!clip_by_global_norm_1/truediv_1/yConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2#
!clip_by_global_norm_1/truediv_1/yÀ
clip_by_global_norm_1/truediv_1RealDiv$clip_by_global_norm_1/Const:output:0*clip_by_global_norm_1/truediv_1/y:output:0*
T0*
_output_shapes
: 2!
clip_by_global_norm_1/truediv_1²
clip_by_global_norm_1/MinimumMinimum!clip_by_global_norm_1/truediv:z:0#clip_by_global_norm_1/truediv_1:z:0*
T0*
_output_shapes
: 2
clip_by_global_norm_1/Minimum
clip_by_global_norm_1/mul/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
clip_by_global_norm_1/mul/x§
clip_by_global_norm_1/mulMul$clip_by_global_norm_1/mul/x:output:0!clip_by_global_norm_1/Minimum:z:0*
T0*
_output_shapes
: 2
clip_by_global_norm_1/mul
clip_by_global_norm_1/subSubglobal_norm_1/global_norm:y:0global_norm_1/global_norm:y:0*
T0*
_output_shapes
: 2
clip_by_global_norm_1/sub
clip_by_global_norm_1/addAddV2clip_by_global_norm_1/mul:z:0clip_by_global_norm_1/sub:z:0*
T0*
_output_shapes
: 2
clip_by_global_norm_1/add¯
clip_by_global_norm_1/mul_1Mul
AddN:sum:0clip_by_global_norm_1/add:z:0*
T0*
_class
	loc:@AddN*
_output_shapes
:	2
clip_by_global_norm_1/mul_1Ð
.clip_by_global_norm_1/clip_by_global_norm_1/_0Identityclip_by_global_norm_1/mul_1:z:0*
T0*
_class
	loc:@AddN*
_output_shapes
:	20
.clip_by_global_norm_1/clip_by_global_norm_1/_0¯
clip_by_global_norm_1/mul_2MulAddN_1:sum:0clip_by_global_norm_1/add:z:0*
T0*
_class
loc:@AddN_1*
_output_shapes	
:2
clip_by_global_norm_1/mul_2Î
.clip_by_global_norm_1/clip_by_global_norm_1/_1Identityclip_by_global_norm_1/mul_2:z:0*
T0*
_class
loc:@AddN_1*
_output_shapes	
:20
.clip_by_global_norm_1/clip_by_global_norm_1/_1´
clip_by_global_norm_1/mul_3MulAddN_2:sum:0clip_by_global_norm_1/add:z:0*
T0*
_class
loc:@AddN_2* 
_output_shapes
:
2
clip_by_global_norm_1/mul_3Ó
.clip_by_global_norm_1/clip_by_global_norm_1/_2Identityclip_by_global_norm_1/mul_3:z:0*
T0*
_class
loc:@AddN_2* 
_output_shapes
:
20
.clip_by_global_norm_1/clip_by_global_norm_1/_2¯
clip_by_global_norm_1/mul_4MulAddN_3:sum:0clip_by_global_norm_1/add:z:0*
T0*
_class
loc:@AddN_3*
_output_shapes	
:2
clip_by_global_norm_1/mul_4Î
.clip_by_global_norm_1/clip_by_global_norm_1/_3Identityclip_by_global_norm_1/mul_4:z:0*
T0*
_class
loc:@AddN_3*
_output_shapes	
:20
.clip_by_global_norm_1/clip_by_global_norm_1/_3³
clip_by_global_norm_1/mul_5MulAddN_4:sum:0clip_by_global_norm_1/add:z:0*
T0*
_class
loc:@AddN_4*
_output_shapes
:	2
clip_by_global_norm_1/mul_5Ò
.clip_by_global_norm_1/clip_by_global_norm_1/_4Identityclip_by_global_norm_1/mul_5:z:0*
T0*
_class
loc:@AddN_4*
_output_shapes
:	20
.clip_by_global_norm_1/clip_by_global_norm_1/_4®
clip_by_global_norm_1/mul_6MulAddN_5:sum:0clip_by_global_norm_1/add:z:0*
T0*
_class
loc:@AddN_5*
_output_shapes
:2
clip_by_global_norm_1/mul_6Í
.clip_by_global_norm_1/clip_by_global_norm_1/_5Identityclip_by_global_norm_1/mul_6:z:0*
T0*
_class
loc:@AddN_5*
_output_shapes
:20
.clip_by_global_norm_1/clip_by_global_norm_1/_5Z

adam/ConstConst*
_output_shapes
: *
dtype0	*
value	B	 R2

adam/Const¦
adam/AssignAddVariableOpAssignAddVariableOp!adam_assignaddvariableop_resourceadam/Const:output:0*
_output_shapes
 *
dtype0	2
adam/AssignAddVariableOp_
adam/Cast/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast/xc
adam/Cast_1/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_1/xc
adam/Cast_2/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_2/xc
adam/Cast_3/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_3/x­
adam/Cast_4/ReadVariableOpReadVariableOp!adam_assignaddvariableop_resource^adam/AssignAddVariableOp*
_output_shapes
: *
dtype0	2
adam/Cast_4/ReadVariableOpv
adam/Cast_4Cast"adam/Cast_4/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_4
adam/mul/ReadVariableOpReadVariableOp adam_mul_readvariableop_resource*
_output_shapes
:	*
dtype02
adam/mul/ReadVariableOp~
adam/mulMuladam/Cast_1/x:output:0adam/mul/ReadVariableOp:value:0*
T0*
_output_shapes
:	2

adam/mul]

adam/sub/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2

adam/sub/xi
adam/subSubadam/sub/x:output:0adam/Cast_1/x:output:0*
T0*
_output_shapes
: 2

adam/sub

adam/mul_1Muladam/sub:z:03clip_by_global_norm/clip_by_global_norm/_0:output:0*
T0*
_output_shapes
:	2

adam/mul_1e
adam/addAddV2adam/mul:z:0adam/mul_1:z:0*
T0*
_output_shapes
:	2

adam/add
adam/mul_2/ReadVariableOpReadVariableOp"adam_mul_2_readvariableop_resource*
_output_shapes
:	*
dtype02
adam/mul_2/ReadVariableOp

adam/mul_2Muladam/Cast_2/x:output:0!adam/mul_2/ReadVariableOp:value:0*
T0*
_output_shapes
:	2

adam/mul_2a
adam/sub_1/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_1/xo

adam/sub_1Subadam/sub_1/x:output:0adam/Cast_2/x:output:0*
T0*
_output_shapes
: 2

adam/sub_1

adam/mul_3Muladam/sub_1:z:03clip_by_global_norm/clip_by_global_norm/_0:output:0*
T0*
_output_shapes
:	2

adam/mul_3

adam/mul_4Muladam/mul_3:z:03clip_by_global_norm/clip_by_global_norm/_0:output:0*
T0*
_output_shapes
:	2

adam/mul_4k

adam/add_1AddV2adam/mul_2:z:0adam/mul_4:z:0*
T0*
_output_shapes
:	2

adam/add_1e
adam/PowPowadam/Cast_1/x:output:0adam/Cast_4:y:0*
T0*
_output_shapes
: 2

adam/Powa
adam/sub_2/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_2/xe

adam/sub_2Subadam/sub_2/x:output:0adam/Pow:z:0*
T0*
_output_shapes
: 2

adam/sub_2o
adam/truedivRealDivadam/add:z:0adam/sub_2:z:0*
T0*
_output_shapes
:	2
adam/truedivi

adam/Pow_1Powadam/Cast_2/x:output:0adam/Cast_4:y:0*
T0*
_output_shapes
: 2

adam/Pow_1a
adam/sub_3/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_3/xg

adam/sub_3Subadam/sub_3/x:output:0adam/Pow_1:z:0*
T0*
_output_shapes
: 2

adam/sub_3u
adam/truediv_1RealDivadam/add_1:z:0adam/sub_3:z:0*
T0*
_output_shapes
:	2
adam/truediv_1q

adam/mul_5Muladam/Cast/x:output:0adam/truediv:z:0*
T0*
_output_shapes
:	2

adam/mul_5\
	adam/SqrtSqrtadam/truediv_1:z:0*
T0*
_output_shapes
:	2
	adam/Sqrtr

adam/add_2AddV2adam/Sqrt:y:0adam/Cast_3/x:output:0*
T0*
_output_shapes
:	2

adam/add_2u
adam/truediv_2RealDivadam/mul_5:z:0adam/add_2:z:0*
T0*
_output_shapes
:	2
adam/truediv_2¥
adam/AssignSubVariableOpAssignSubVariableOp9actor_sequential_1_dense_3_matmul_readvariableop_resourceadam/truediv_2:z:01^actor/sequential_1/dense_3/MatMul/ReadVariableOp3^actor/sequential_1/dense_3/MatMul_1/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp¯
adam/AssignVariableOpAssignVariableOp adam_mul_readvariableop_resourceadam/add:z:0^adam/mul/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp¹
adam/AssignVariableOp_1AssignVariableOp"adam_mul_2_readvariableop_resourceadam/add_1:z:0^adam/mul_2/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_1c
adam/Cast_5/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_5/xc
adam/Cast_6/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_6/xc
adam/Cast_7/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_7/xc
adam/Cast_8/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_8/x­
adam/Cast_9/ReadVariableOpReadVariableOp!adam_assignaddvariableop_resource^adam/AssignAddVariableOp*
_output_shapes
: *
dtype0	2
adam/Cast_9/ReadVariableOpv
adam/Cast_9Cast"adam/Cast_9/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_9
adam/mul_6/ReadVariableOpReadVariableOp"adam_mul_6_readvariableop_resource*
_output_shapes	
:*
dtype02
adam/mul_6/ReadVariableOp

adam/mul_6Muladam/Cast_6/x:output:0!adam/mul_6/ReadVariableOp:value:0*
T0*
_output_shapes	
:2

adam/mul_6a
adam/sub_4/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_4/xo

adam/sub_4Subadam/sub_4/x:output:0adam/Cast_6/x:output:0*
T0*
_output_shapes
: 2

adam/sub_4

adam/mul_7Muladam/sub_4:z:03clip_by_global_norm/clip_by_global_norm/_1:output:0*
T0*
_output_shapes	
:2

adam/mul_7g

adam/add_3AddV2adam/mul_6:z:0adam/mul_7:z:0*
T0*
_output_shapes	
:2

adam/add_3
adam/mul_8/ReadVariableOpReadVariableOp"adam_mul_8_readvariableop_resource*
_output_shapes	
:*
dtype02
adam/mul_8/ReadVariableOp

adam/mul_8Muladam/Cast_7/x:output:0!adam/mul_8/ReadVariableOp:value:0*
T0*
_output_shapes	
:2

adam/mul_8a
adam/sub_5/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_5/xo

adam/sub_5Subadam/sub_5/x:output:0adam/Cast_7/x:output:0*
T0*
_output_shapes
: 2

adam/sub_5

adam/mul_9Muladam/sub_5:z:03clip_by_global_norm/clip_by_global_norm/_1:output:0*
T0*
_output_shapes	
:2

adam/mul_9
adam/mul_10Muladam/mul_9:z:03clip_by_global_norm/clip_by_global_norm/_1:output:0*
T0*
_output_shapes	
:2
adam/mul_10h

adam/add_4AddV2adam/mul_8:z:0adam/mul_10:z:0*
T0*
_output_shapes	
:2

adam/add_4i

adam/Pow_2Powadam/Cast_6/x:output:0adam/Cast_9:y:0*
T0*
_output_shapes
: 2

adam/Pow_2a
adam/sub_6/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_6/xg

adam/sub_6Subadam/sub_6/x:output:0adam/Pow_2:z:0*
T0*
_output_shapes
: 2

adam/sub_6q
adam/truediv_3RealDivadam/add_3:z:0adam/sub_6:z:0*
T0*
_output_shapes	
:2
adam/truediv_3i

adam/Pow_3Powadam/Cast_7/x:output:0adam/Cast_9:y:0*
T0*
_output_shapes
: 2

adam/Pow_3a
adam/sub_7/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_7/xg

adam/sub_7Subadam/sub_7/x:output:0adam/Pow_3:z:0*
T0*
_output_shapes
: 2

adam/sub_7q
adam/truediv_4RealDivadam/add_4:z:0adam/sub_7:z:0*
T0*
_output_shapes	
:2
adam/truediv_4s
adam/mul_11Muladam/Cast_5/x:output:0adam/truediv_3:z:0*
T0*
_output_shapes	
:2
adam/mul_11\
adam/Sqrt_1Sqrtadam/truediv_4:z:0*
T0*
_output_shapes	
:2
adam/Sqrt_1p

adam/add_5AddV2adam/Sqrt_1:y:0adam/Cast_8/x:output:0*
T0*
_output_shapes	
:2

adam/add_5r
adam/truediv_5RealDivadam/mul_11:z:0adam/add_5:z:0*
T0*
_output_shapes	
:2
adam/truediv_5¬
adam/AssignSubVariableOp_1AssignSubVariableOp:actor_sequential_1_dense_3_biasadd_readvariableop_resourceadam/truediv_5:z:02^actor/sequential_1/dense_3/BiasAdd/ReadVariableOp4^actor/sequential_1/dense_3/BiasAdd_1/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_1¹
adam/AssignVariableOp_2AssignVariableOp"adam_mul_6_readvariableop_resourceadam/add_3:z:0^adam/mul_6/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_2¹
adam/AssignVariableOp_3AssignVariableOp"adam_mul_8_readvariableop_resourceadam/add_4:z:0^adam/mul_8/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_3e
adam/Cast_10/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_10/xe
adam/Cast_11/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_11/xe
adam/Cast_12/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_12/xe
adam/Cast_13/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_13/x¯
adam/Cast_14/ReadVariableOpReadVariableOp!adam_assignaddvariableop_resource^adam/AssignAddVariableOp*
_output_shapes
: *
dtype0	2
adam/Cast_14/ReadVariableOpy
adam/Cast_14Cast#adam/Cast_14/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_14
adam/mul_12/ReadVariableOpReadVariableOp#adam_mul_12_readvariableop_resource* 
_output_shapes
:
*
dtype02
adam/mul_12/ReadVariableOp
adam/mul_12Muladam/Cast_11/x:output:0"adam/mul_12/ReadVariableOp:value:0*
T0* 
_output_shapes
:
2
adam/mul_12a
adam/sub_8/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_8/xp

adam/sub_8Subadam/sub_8/x:output:0adam/Cast_11/x:output:0*
T0*
_output_shapes
: 2

adam/sub_8
adam/mul_13Muladam/sub_8:z:03clip_by_global_norm/clip_by_global_norm/_2:output:0*
T0* 
_output_shapes
:
2
adam/mul_13n

adam/add_6AddV2adam/mul_12:z:0adam/mul_13:z:0*
T0* 
_output_shapes
:
2

adam/add_6
adam/mul_14/ReadVariableOpReadVariableOp#adam_mul_14_readvariableop_resource* 
_output_shapes
:
*
dtype02
adam/mul_14/ReadVariableOp
adam/mul_14Muladam/Cast_12/x:output:0"adam/mul_14/ReadVariableOp:value:0*
T0* 
_output_shapes
:
2
adam/mul_14a
adam/sub_9/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_9/xp

adam/sub_9Subadam/sub_9/x:output:0adam/Cast_12/x:output:0*
T0*
_output_shapes
: 2

adam/sub_9
adam/mul_15Muladam/sub_9:z:03clip_by_global_norm/clip_by_global_norm/_2:output:0*
T0* 
_output_shapes
:
2
adam/mul_15
adam/mul_16Muladam/mul_15:z:03clip_by_global_norm/clip_by_global_norm/_2:output:0*
T0* 
_output_shapes
:
2
adam/mul_16n

adam/add_7AddV2adam/mul_14:z:0adam/mul_16:z:0*
T0* 
_output_shapes
:
2

adam/add_7k

adam/Pow_4Powadam/Cast_11/x:output:0adam/Cast_14:y:0*
T0*
_output_shapes
: 2

adam/Pow_4c
adam/sub_10/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_10/xj
adam/sub_10Subadam/sub_10/x:output:0adam/Pow_4:z:0*
T0*
_output_shapes
: 2
adam/sub_10w
adam/truediv_6RealDivadam/add_6:z:0adam/sub_10:z:0*
T0* 
_output_shapes
:
2
adam/truediv_6k

adam/Pow_5Powadam/Cast_12/x:output:0adam/Cast_14:y:0*
T0*
_output_shapes
: 2

adam/Pow_5c
adam/sub_11/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_11/xj
adam/sub_11Subadam/sub_11/x:output:0adam/Pow_5:z:0*
T0*
_output_shapes
: 2
adam/sub_11w
adam/truediv_7RealDivadam/add_7:z:0adam/sub_11:z:0*
T0* 
_output_shapes
:
2
adam/truediv_7y
adam/mul_17Muladam/Cast_10/x:output:0adam/truediv_6:z:0*
T0* 
_output_shapes
:
2
adam/mul_17a
adam/Sqrt_2Sqrtadam/truediv_7:z:0*
T0* 
_output_shapes
:
2
adam/Sqrt_2v

adam/add_8AddV2adam/Sqrt_2:y:0adam/Cast_13/x:output:0*
T0* 
_output_shapes
:
2

adam/add_8w
adam/truediv_8RealDivadam/mul_17:z:0adam/add_8:z:0*
T0* 
_output_shapes
:
2
adam/truediv_8©
adam/AssignSubVariableOp_2AssignSubVariableOp9actor_sequential_1_dense_4_matmul_readvariableop_resourceadam/truediv_8:z:01^actor/sequential_1/dense_4/MatMul/ReadVariableOp3^actor/sequential_1/dense_4/MatMul_1/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_2»
adam/AssignVariableOp_4AssignVariableOp#adam_mul_12_readvariableop_resourceadam/add_6:z:0^adam/mul_12/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_4»
adam/AssignVariableOp_5AssignVariableOp#adam_mul_14_readvariableop_resourceadam/add_7:z:0^adam/mul_14/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_5e
adam/Cast_15/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_15/xe
adam/Cast_16/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_16/xe
adam/Cast_17/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_17/xe
adam/Cast_18/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_18/x¯
adam/Cast_19/ReadVariableOpReadVariableOp!adam_assignaddvariableop_resource^adam/AssignAddVariableOp*
_output_shapes
: *
dtype0	2
adam/Cast_19/ReadVariableOpy
adam/Cast_19Cast#adam/Cast_19/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_19
adam/mul_18/ReadVariableOpReadVariableOp#adam_mul_18_readvariableop_resource*
_output_shapes	
:*
dtype02
adam/mul_18/ReadVariableOp
adam/mul_18Muladam/Cast_16/x:output:0"adam/mul_18/ReadVariableOp:value:0*
T0*
_output_shapes	
:2
adam/mul_18c
adam/sub_12/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_12/xs
adam/sub_12Subadam/sub_12/x:output:0adam/Cast_16/x:output:0*
T0*
_output_shapes
: 2
adam/sub_12
adam/mul_19Muladam/sub_12:z:03clip_by_global_norm/clip_by_global_norm/_3:output:0*
T0*
_output_shapes	
:2
adam/mul_19i

adam/add_9AddV2adam/mul_18:z:0adam/mul_19:z:0*
T0*
_output_shapes	
:2

adam/add_9
adam/mul_20/ReadVariableOpReadVariableOp#adam_mul_20_readvariableop_resource*
_output_shapes	
:*
dtype02
adam/mul_20/ReadVariableOp
adam/mul_20Muladam/Cast_17/x:output:0"adam/mul_20/ReadVariableOp:value:0*
T0*
_output_shapes	
:2
adam/mul_20c
adam/sub_13/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_13/xs
adam/sub_13Subadam/sub_13/x:output:0adam/Cast_17/x:output:0*
T0*
_output_shapes
: 2
adam/sub_13
adam/mul_21Muladam/sub_13:z:03clip_by_global_norm/clip_by_global_norm/_3:output:0*
T0*
_output_shapes	
:2
adam/mul_21
adam/mul_22Muladam/mul_21:z:03clip_by_global_norm/clip_by_global_norm/_3:output:0*
T0*
_output_shapes	
:2
adam/mul_22k
adam/add_10AddV2adam/mul_20:z:0adam/mul_22:z:0*
T0*
_output_shapes	
:2
adam/add_10k

adam/Pow_6Powadam/Cast_16/x:output:0adam/Cast_19:y:0*
T0*
_output_shapes
: 2

adam/Pow_6c
adam/sub_14/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_14/xj
adam/sub_14Subadam/sub_14/x:output:0adam/Pow_6:z:0*
T0*
_output_shapes
: 2
adam/sub_14r
adam/truediv_9RealDivadam/add_9:z:0adam/sub_14:z:0*
T0*
_output_shapes	
:2
adam/truediv_9k

adam/Pow_7Powadam/Cast_17/x:output:0adam/Cast_19:y:0*
T0*
_output_shapes
: 2

adam/Pow_7c
adam/sub_15/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_15/xj
adam/sub_15Subadam/sub_15/x:output:0adam/Pow_7:z:0*
T0*
_output_shapes
: 2
adam/sub_15u
adam/truediv_10RealDivadam/add_10:z:0adam/sub_15:z:0*
T0*
_output_shapes	
:2
adam/truediv_10t
adam/mul_23Muladam/Cast_15/x:output:0adam/truediv_9:z:0*
T0*
_output_shapes	
:2
adam/mul_23]
adam/Sqrt_3Sqrtadam/truediv_10:z:0*
T0*
_output_shapes	
:2
adam/Sqrt_3s
adam/add_11AddV2adam/Sqrt_3:y:0adam/Cast_18/x:output:0*
T0*
_output_shapes	
:2
adam/add_11u
adam/truediv_11RealDivadam/mul_23:z:0adam/add_11:z:0*
T0*
_output_shapes	
:2
adam/truediv_11­
adam/AssignSubVariableOp_3AssignSubVariableOp:actor_sequential_1_dense_4_biasadd_readvariableop_resourceadam/truediv_11:z:02^actor/sequential_1/dense_4/BiasAdd/ReadVariableOp4^actor/sequential_1/dense_4/BiasAdd_1/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_3»
adam/AssignVariableOp_6AssignVariableOp#adam_mul_18_readvariableop_resourceadam/add_9:z:0^adam/mul_18/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_6¼
adam/AssignVariableOp_7AssignVariableOp#adam_mul_20_readvariableop_resourceadam/add_10:z:0^adam/mul_20/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_7e
adam/Cast_20/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_20/xe
adam/Cast_21/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_21/xe
adam/Cast_22/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_22/xe
adam/Cast_23/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_23/x¯
adam/Cast_24/ReadVariableOpReadVariableOp!adam_assignaddvariableop_resource^adam/AssignAddVariableOp*
_output_shapes
: *
dtype0	2
adam/Cast_24/ReadVariableOpy
adam/Cast_24Cast#adam/Cast_24/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_24
adam/mul_24/ReadVariableOpReadVariableOp#adam_mul_24_readvariableop_resource*
_output_shapes
:	*
dtype02
adam/mul_24/ReadVariableOp
adam/mul_24Muladam/Cast_21/x:output:0"adam/mul_24/ReadVariableOp:value:0*
T0*
_output_shapes
:	2
adam/mul_24c
adam/sub_16/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_16/xs
adam/sub_16Subadam/sub_16/x:output:0adam/Cast_21/x:output:0*
T0*
_output_shapes
: 2
adam/sub_16
adam/mul_25Muladam/sub_16:z:03clip_by_global_norm/clip_by_global_norm/_4:output:0*
T0*
_output_shapes
:	2
adam/mul_25o
adam/add_12AddV2adam/mul_24:z:0adam/mul_25:z:0*
T0*
_output_shapes
:	2
adam/add_12
adam/mul_26/ReadVariableOpReadVariableOp#adam_mul_26_readvariableop_resource*
_output_shapes
:	*
dtype02
adam/mul_26/ReadVariableOp
adam/mul_26Muladam/Cast_22/x:output:0"adam/mul_26/ReadVariableOp:value:0*
T0*
_output_shapes
:	2
adam/mul_26c
adam/sub_17/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_17/xs
adam/sub_17Subadam/sub_17/x:output:0adam/Cast_22/x:output:0*
T0*
_output_shapes
: 2
adam/sub_17
adam/mul_27Muladam/sub_17:z:03clip_by_global_norm/clip_by_global_norm/_4:output:0*
T0*
_output_shapes
:	2
adam/mul_27
adam/mul_28Muladam/mul_27:z:03clip_by_global_norm/clip_by_global_norm/_4:output:0*
T0*
_output_shapes
:	2
adam/mul_28o
adam/add_13AddV2adam/mul_26:z:0adam/mul_28:z:0*
T0*
_output_shapes
:	2
adam/add_13k

adam/Pow_8Powadam/Cast_21/x:output:0adam/Cast_24:y:0*
T0*
_output_shapes
: 2

adam/Pow_8c
adam/sub_18/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_18/xj
adam/sub_18Subadam/sub_18/x:output:0adam/Pow_8:z:0*
T0*
_output_shapes
: 2
adam/sub_18y
adam/truediv_12RealDivadam/add_12:z:0adam/sub_18:z:0*
T0*
_output_shapes
:	2
adam/truediv_12k

adam/Pow_9Powadam/Cast_22/x:output:0adam/Cast_24:y:0*
T0*
_output_shapes
: 2

adam/Pow_9c
adam/sub_19/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_19/xj
adam/sub_19Subadam/sub_19/x:output:0adam/Pow_9:z:0*
T0*
_output_shapes
: 2
adam/sub_19y
adam/truediv_13RealDivadam/add_13:z:0adam/sub_19:z:0*
T0*
_output_shapes
:	2
adam/truediv_13y
adam/mul_29Muladam/Cast_20/x:output:0adam/truediv_12:z:0*
T0*
_output_shapes
:	2
adam/mul_29a
adam/Sqrt_4Sqrtadam/truediv_13:z:0*
T0*
_output_shapes
:	2
adam/Sqrt_4w
adam/add_14AddV2adam/Sqrt_4:y:0adam/Cast_23/x:output:0*
T0*
_output_shapes
:	2
adam/add_14y
adam/truediv_14RealDivadam/mul_29:z:0adam/add_14:z:0*
T0*
_output_shapes
:	2
adam/truediv_14ª
adam/AssignSubVariableOp_4AssignSubVariableOp9actor_sequential_1_dense_5_matmul_readvariableop_resourceadam/truediv_14:z:01^actor/sequential_1/dense_5/MatMul/ReadVariableOp3^actor/sequential_1/dense_5/MatMul_1/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_4¼
adam/AssignVariableOp_8AssignVariableOp#adam_mul_24_readvariableop_resourceadam/add_12:z:0^adam/mul_24/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_8¼
adam/AssignVariableOp_9AssignVariableOp#adam_mul_26_readvariableop_resourceadam/add_13:z:0^adam/mul_26/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_9e
adam/Cast_25/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_25/xe
adam/Cast_26/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_26/xe
adam/Cast_27/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_27/xe
adam/Cast_28/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_28/x¯
adam/Cast_29/ReadVariableOpReadVariableOp!adam_assignaddvariableop_resource^adam/AssignAddVariableOp*
_output_shapes
: *
dtype0	2
adam/Cast_29/ReadVariableOpy
adam/Cast_29Cast#adam/Cast_29/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_29
adam/mul_30/ReadVariableOpReadVariableOp#adam_mul_30_readvariableop_resource*
_output_shapes
:*
dtype02
adam/mul_30/ReadVariableOp
adam/mul_30Muladam/Cast_26/x:output:0"adam/mul_30/ReadVariableOp:value:0*
T0*
_output_shapes
:2
adam/mul_30c
adam/sub_20/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_20/xs
adam/sub_20Subadam/sub_20/x:output:0adam/Cast_26/x:output:0*
T0*
_output_shapes
: 2
adam/sub_20
adam/mul_31Muladam/sub_20:z:03clip_by_global_norm/clip_by_global_norm/_5:output:0*
T0*
_output_shapes
:2
adam/mul_31j
adam/add_15AddV2adam/mul_30:z:0adam/mul_31:z:0*
T0*
_output_shapes
:2
adam/add_15
adam/mul_32/ReadVariableOpReadVariableOp#adam_mul_32_readvariableop_resource*
_output_shapes
:*
dtype02
adam/mul_32/ReadVariableOp
adam/mul_32Muladam/Cast_27/x:output:0"adam/mul_32/ReadVariableOp:value:0*
T0*
_output_shapes
:2
adam/mul_32c
adam/sub_21/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_21/xs
adam/sub_21Subadam/sub_21/x:output:0adam/Cast_27/x:output:0*
T0*
_output_shapes
: 2
adam/sub_21
adam/mul_33Muladam/sub_21:z:03clip_by_global_norm/clip_by_global_norm/_5:output:0*
T0*
_output_shapes
:2
adam/mul_33
adam/mul_34Muladam/mul_33:z:03clip_by_global_norm/clip_by_global_norm/_5:output:0*
T0*
_output_shapes
:2
adam/mul_34j
adam/add_16AddV2adam/mul_32:z:0adam/mul_34:z:0*
T0*
_output_shapes
:2
adam/add_16m
adam/Pow_10Powadam/Cast_26/x:output:0adam/Cast_29:y:0*
T0*
_output_shapes
: 2
adam/Pow_10c
adam/sub_22/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_22/xk
adam/sub_22Subadam/sub_22/x:output:0adam/Pow_10:z:0*
T0*
_output_shapes
: 2
adam/sub_22t
adam/truediv_15RealDivadam/add_15:z:0adam/sub_22:z:0*
T0*
_output_shapes
:2
adam/truediv_15m
adam/Pow_11Powadam/Cast_27/x:output:0adam/Cast_29:y:0*
T0*
_output_shapes
: 2
adam/Pow_11c
adam/sub_23/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_23/xk
adam/sub_23Subadam/sub_23/x:output:0adam/Pow_11:z:0*
T0*
_output_shapes
: 2
adam/sub_23t
adam/truediv_16RealDivadam/add_16:z:0adam/sub_23:z:0*
T0*
_output_shapes
:2
adam/truediv_16t
adam/mul_35Muladam/Cast_25/x:output:0adam/truediv_15:z:0*
T0*
_output_shapes
:2
adam/mul_35\
adam/Sqrt_5Sqrtadam/truediv_16:z:0*
T0*
_output_shapes
:2
adam/Sqrt_5r
adam/add_17AddV2adam/Sqrt_5:y:0adam/Cast_28/x:output:0*
T0*
_output_shapes
:2
adam/add_17t
adam/truediv_17RealDivadam/mul_35:z:0adam/add_17:z:0*
T0*
_output_shapes
:2
adam/truediv_17­
adam/AssignSubVariableOp_5AssignSubVariableOp:actor_sequential_1_dense_5_biasadd_readvariableop_resourceadam/truediv_17:z:02^actor/sequential_1/dense_5/BiasAdd/ReadVariableOp4^actor/sequential_1/dense_5/BiasAdd_1/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_5¾
adam/AssignVariableOp_10AssignVariableOp#adam_mul_30_readvariableop_resourceadam/add_15:z:0^adam/mul_30/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_10¾
adam/AssignVariableOp_11AssignVariableOp#adam_mul_32_readvariableop_resourceadam/add_16:z:0^adam/mul_32/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_11^
adam/Const_1Const*
_output_shapes
: *
dtype0	*
value	B	 R2
adam/Const_1®
adam/AssignAddVariableOp_1AssignAddVariableOp#adam_assignaddvariableop_1_resourceadam/Const_1:output:0*
_output_shapes
 *
dtype0	2
adam/AssignAddVariableOp_1e
adam/Cast_30/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_30/xe
adam/Cast_31/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_31/xe
adam/Cast_32/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_32/xe
adam/Cast_33/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_33/x³
adam/Cast_34/ReadVariableOpReadVariableOp#adam_assignaddvariableop_1_resource^adam/AssignAddVariableOp_1*
_output_shapes
: *
dtype0	2
adam/Cast_34/ReadVariableOpy
adam/Cast_34Cast#adam/Cast_34/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_34
adam/mul_36/ReadVariableOpReadVariableOp#adam_mul_36_readvariableop_resource*
_output_shapes
:	*
dtype02
adam/mul_36/ReadVariableOp
adam/mul_36Muladam/Cast_31/x:output:0"adam/mul_36/ReadVariableOp:value:0*
T0*
_output_shapes
:	2
adam/mul_36c
adam/sub_24/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_24/xs
adam/sub_24Subadam/sub_24/x:output:0adam/Cast_31/x:output:0*
T0*
_output_shapes
: 2
adam/sub_24
adam/mul_37Muladam/sub_24:z:07clip_by_global_norm_1/clip_by_global_norm_1/_0:output:0*
T0*
_output_shapes
:	2
adam/mul_37o
adam/add_18AddV2adam/mul_36:z:0adam/mul_37:z:0*
T0*
_output_shapes
:	2
adam/add_18
adam/mul_38/ReadVariableOpReadVariableOp#adam_mul_38_readvariableop_resource*
_output_shapes
:	*
dtype02
adam/mul_38/ReadVariableOp
adam/mul_38Muladam/Cast_32/x:output:0"adam/mul_38/ReadVariableOp:value:0*
T0*
_output_shapes
:	2
adam/mul_38c
adam/sub_25/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_25/xs
adam/sub_25Subadam/sub_25/x:output:0adam/Cast_32/x:output:0*
T0*
_output_shapes
: 2
adam/sub_25
adam/mul_39Muladam/sub_25:z:07clip_by_global_norm_1/clip_by_global_norm_1/_0:output:0*
T0*
_output_shapes
:	2
adam/mul_39
adam/mul_40Muladam/mul_39:z:07clip_by_global_norm_1/clip_by_global_norm_1/_0:output:0*
T0*
_output_shapes
:	2
adam/mul_40o
adam/add_19AddV2adam/mul_38:z:0adam/mul_40:z:0*
T0*
_output_shapes
:	2
adam/add_19m
adam/Pow_12Powadam/Cast_31/x:output:0adam/Cast_34:y:0*
T0*
_output_shapes
: 2
adam/Pow_12c
adam/sub_26/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_26/xk
adam/sub_26Subadam/sub_26/x:output:0adam/Pow_12:z:0*
T0*
_output_shapes
: 2
adam/sub_26y
adam/truediv_18RealDivadam/add_18:z:0adam/sub_26:z:0*
T0*
_output_shapes
:	2
adam/truediv_18m
adam/Pow_13Powadam/Cast_32/x:output:0adam/Cast_34:y:0*
T0*
_output_shapes
: 2
adam/Pow_13c
adam/sub_27/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_27/xk
adam/sub_27Subadam/sub_27/x:output:0adam/Pow_13:z:0*
T0*
_output_shapes
: 2
adam/sub_27y
adam/truediv_19RealDivadam/add_19:z:0adam/sub_27:z:0*
T0*
_output_shapes
:	2
adam/truediv_19y
adam/mul_41Muladam/Cast_30/x:output:0adam/truediv_18:z:0*
T0*
_output_shapes
:	2
adam/mul_41a
adam/Sqrt_6Sqrtadam/truediv_19:z:0*
T0*
_output_shapes
:	2
adam/Sqrt_6w
adam/add_20AddV2adam/Sqrt_6:y:0adam/Cast_33/x:output:0*
T0*
_output_shapes
:	2
adam/add_20y
adam/truediv_20RealDivadam/mul_41:z:0adam/add_20:z:0*
T0*
_output_shapes
:	2
adam/truediv_20Ó
adam/AssignSubVariableOp_6AssignSubVariableOp6critic_sequential_dense_matmul_readvariableop_resourceadam/truediv_20:z:0.^critic/sequential/dense/MatMul/ReadVariableOp0^critic/sequential/dense/MatMul_1/ReadVariableOp0^critic/sequential/dense/MatMul_2/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_6¾
adam/AssignVariableOp_12AssignVariableOp#adam_mul_36_readvariableop_resourceadam/add_18:z:0^adam/mul_36/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_12¾
adam/AssignVariableOp_13AssignVariableOp#adam_mul_38_readvariableop_resourceadam/add_19:z:0^adam/mul_38/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_13e
adam/Cast_35/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_35/xe
adam/Cast_36/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_36/xe
adam/Cast_37/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_37/xe
adam/Cast_38/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_38/x³
adam/Cast_39/ReadVariableOpReadVariableOp#adam_assignaddvariableop_1_resource^adam/AssignAddVariableOp_1*
_output_shapes
: *
dtype0	2
adam/Cast_39/ReadVariableOpy
adam/Cast_39Cast#adam/Cast_39/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_39
adam/mul_42/ReadVariableOpReadVariableOp#adam_mul_42_readvariableop_resource*
_output_shapes	
:*
dtype02
adam/mul_42/ReadVariableOp
adam/mul_42Muladam/Cast_36/x:output:0"adam/mul_42/ReadVariableOp:value:0*
T0*
_output_shapes	
:2
adam/mul_42c
adam/sub_28/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_28/xs
adam/sub_28Subadam/sub_28/x:output:0adam/Cast_36/x:output:0*
T0*
_output_shapes
: 2
adam/sub_28
adam/mul_43Muladam/sub_28:z:07clip_by_global_norm_1/clip_by_global_norm_1/_1:output:0*
T0*
_output_shapes	
:2
adam/mul_43k
adam/add_21AddV2adam/mul_42:z:0adam/mul_43:z:0*
T0*
_output_shapes	
:2
adam/add_21
adam/mul_44/ReadVariableOpReadVariableOp#adam_mul_44_readvariableop_resource*
_output_shapes	
:*
dtype02
adam/mul_44/ReadVariableOp
adam/mul_44Muladam/Cast_37/x:output:0"adam/mul_44/ReadVariableOp:value:0*
T0*
_output_shapes	
:2
adam/mul_44c
adam/sub_29/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_29/xs
adam/sub_29Subadam/sub_29/x:output:0adam/Cast_37/x:output:0*
T0*
_output_shapes
: 2
adam/sub_29
adam/mul_45Muladam/sub_29:z:07clip_by_global_norm_1/clip_by_global_norm_1/_1:output:0*
T0*
_output_shapes	
:2
adam/mul_45
adam/mul_46Muladam/mul_45:z:07clip_by_global_norm_1/clip_by_global_norm_1/_1:output:0*
T0*
_output_shapes	
:2
adam/mul_46k
adam/add_22AddV2adam/mul_44:z:0adam/mul_46:z:0*
T0*
_output_shapes	
:2
adam/add_22m
adam/Pow_14Powadam/Cast_36/x:output:0adam/Cast_39:y:0*
T0*
_output_shapes
: 2
adam/Pow_14c
adam/sub_30/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_30/xk
adam/sub_30Subadam/sub_30/x:output:0adam/Pow_14:z:0*
T0*
_output_shapes
: 2
adam/sub_30u
adam/truediv_21RealDivadam/add_21:z:0adam/sub_30:z:0*
T0*
_output_shapes	
:2
adam/truediv_21m
adam/Pow_15Powadam/Cast_37/x:output:0adam/Cast_39:y:0*
T0*
_output_shapes
: 2
adam/Pow_15c
adam/sub_31/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_31/xk
adam/sub_31Subadam/sub_31/x:output:0adam/Pow_15:z:0*
T0*
_output_shapes
: 2
adam/sub_31u
adam/truediv_22RealDivadam/add_22:z:0adam/sub_31:z:0*
T0*
_output_shapes	
:2
adam/truediv_22u
adam/mul_47Muladam/Cast_35/x:output:0adam/truediv_21:z:0*
T0*
_output_shapes	
:2
adam/mul_47]
adam/Sqrt_7Sqrtadam/truediv_22:z:0*
T0*
_output_shapes	
:2
adam/Sqrt_7s
adam/add_23AddV2adam/Sqrt_7:y:0adam/Cast_38/x:output:0*
T0*
_output_shapes	
:2
adam/add_23u
adam/truediv_23RealDivadam/mul_47:z:0adam/add_23:z:0*
T0*
_output_shapes	
:2
adam/truediv_23×
adam/AssignSubVariableOp_7AssignSubVariableOp7critic_sequential_dense_biasadd_readvariableop_resourceadam/truediv_23:z:0/^critic/sequential/dense/BiasAdd/ReadVariableOp1^critic/sequential/dense/BiasAdd_1/ReadVariableOp1^critic/sequential/dense/BiasAdd_2/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_7¾
adam/AssignVariableOp_14AssignVariableOp#adam_mul_42_readvariableop_resourceadam/add_21:z:0^adam/mul_42/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_14¾
adam/AssignVariableOp_15AssignVariableOp#adam_mul_44_readvariableop_resourceadam/add_22:z:0^adam/mul_44/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_15e
adam/Cast_40/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_40/xe
adam/Cast_41/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_41/xe
adam/Cast_42/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_42/xe
adam/Cast_43/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_43/x³
adam/Cast_44/ReadVariableOpReadVariableOp#adam_assignaddvariableop_1_resource^adam/AssignAddVariableOp_1*
_output_shapes
: *
dtype0	2
adam/Cast_44/ReadVariableOpy
adam/Cast_44Cast#adam/Cast_44/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_44
adam/mul_48/ReadVariableOpReadVariableOp#adam_mul_48_readvariableop_resource* 
_output_shapes
:
*
dtype02
adam/mul_48/ReadVariableOp
adam/mul_48Muladam/Cast_41/x:output:0"adam/mul_48/ReadVariableOp:value:0*
T0* 
_output_shapes
:
2
adam/mul_48c
adam/sub_32/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_32/xs
adam/sub_32Subadam/sub_32/x:output:0adam/Cast_41/x:output:0*
T0*
_output_shapes
: 2
adam/sub_32
adam/mul_49Muladam/sub_32:z:07clip_by_global_norm_1/clip_by_global_norm_1/_2:output:0*
T0* 
_output_shapes
:
2
adam/mul_49p
adam/add_24AddV2adam/mul_48:z:0adam/mul_49:z:0*
T0* 
_output_shapes
:
2
adam/add_24
adam/mul_50/ReadVariableOpReadVariableOp#adam_mul_50_readvariableop_resource* 
_output_shapes
:
*
dtype02
adam/mul_50/ReadVariableOp
adam/mul_50Muladam/Cast_42/x:output:0"adam/mul_50/ReadVariableOp:value:0*
T0* 
_output_shapes
:
2
adam/mul_50c
adam/sub_33/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_33/xs
adam/sub_33Subadam/sub_33/x:output:0adam/Cast_42/x:output:0*
T0*
_output_shapes
: 2
adam/sub_33
adam/mul_51Muladam/sub_33:z:07clip_by_global_norm_1/clip_by_global_norm_1/_2:output:0*
T0* 
_output_shapes
:
2
adam/mul_51
adam/mul_52Muladam/mul_51:z:07clip_by_global_norm_1/clip_by_global_norm_1/_2:output:0*
T0* 
_output_shapes
:
2
adam/mul_52p
adam/add_25AddV2adam/mul_50:z:0adam/mul_52:z:0*
T0* 
_output_shapes
:
2
adam/add_25m
adam/Pow_16Powadam/Cast_41/x:output:0adam/Cast_44:y:0*
T0*
_output_shapes
: 2
adam/Pow_16c
adam/sub_34/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_34/xk
adam/sub_34Subadam/sub_34/x:output:0adam/Pow_16:z:0*
T0*
_output_shapes
: 2
adam/sub_34z
adam/truediv_24RealDivadam/add_24:z:0adam/sub_34:z:0*
T0* 
_output_shapes
:
2
adam/truediv_24m
adam/Pow_17Powadam/Cast_42/x:output:0adam/Cast_44:y:0*
T0*
_output_shapes
: 2
adam/Pow_17c
adam/sub_35/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_35/xk
adam/sub_35Subadam/sub_35/x:output:0adam/Pow_17:z:0*
T0*
_output_shapes
: 2
adam/sub_35z
adam/truediv_25RealDivadam/add_25:z:0adam/sub_35:z:0*
T0* 
_output_shapes
:
2
adam/truediv_25z
adam/mul_53Muladam/Cast_40/x:output:0adam/truediv_24:z:0*
T0* 
_output_shapes
:
2
adam/mul_53b
adam/Sqrt_8Sqrtadam/truediv_25:z:0*
T0* 
_output_shapes
:
2
adam/Sqrt_8x
adam/add_26AddV2adam/Sqrt_8:y:0adam/Cast_43/x:output:0*
T0* 
_output_shapes
:
2
adam/add_26z
adam/truediv_26RealDivadam/mul_53:z:0adam/add_26:z:0*
T0* 
_output_shapes
:
2
adam/truediv_26Û
adam/AssignSubVariableOp_8AssignSubVariableOp8critic_sequential_dense_1_matmul_readvariableop_resourceadam/truediv_26:z:00^critic/sequential/dense_1/MatMul/ReadVariableOp2^critic/sequential/dense_1/MatMul_1/ReadVariableOp2^critic/sequential/dense_1/MatMul_2/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_8¾
adam/AssignVariableOp_16AssignVariableOp#adam_mul_48_readvariableop_resourceadam/add_24:z:0^adam/mul_48/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_16¾
adam/AssignVariableOp_17AssignVariableOp#adam_mul_50_readvariableop_resourceadam/add_25:z:0^adam/mul_50/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_17e
adam/Cast_45/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_45/xe
adam/Cast_46/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_46/xe
adam/Cast_47/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_47/xe
adam/Cast_48/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_48/x³
adam/Cast_49/ReadVariableOpReadVariableOp#adam_assignaddvariableop_1_resource^adam/AssignAddVariableOp_1*
_output_shapes
: *
dtype0	2
adam/Cast_49/ReadVariableOpy
adam/Cast_49Cast#adam/Cast_49/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_49
adam/mul_54/ReadVariableOpReadVariableOp#adam_mul_54_readvariableop_resource*
_output_shapes	
:*
dtype02
adam/mul_54/ReadVariableOp
adam/mul_54Muladam/Cast_46/x:output:0"adam/mul_54/ReadVariableOp:value:0*
T0*
_output_shapes	
:2
adam/mul_54c
adam/sub_36/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_36/xs
adam/sub_36Subadam/sub_36/x:output:0adam/Cast_46/x:output:0*
T0*
_output_shapes
: 2
adam/sub_36
adam/mul_55Muladam/sub_36:z:07clip_by_global_norm_1/clip_by_global_norm_1/_3:output:0*
T0*
_output_shapes	
:2
adam/mul_55k
adam/add_27AddV2adam/mul_54:z:0adam/mul_55:z:0*
T0*
_output_shapes	
:2
adam/add_27
adam/mul_56/ReadVariableOpReadVariableOp#adam_mul_56_readvariableop_resource*
_output_shapes	
:*
dtype02
adam/mul_56/ReadVariableOp
adam/mul_56Muladam/Cast_47/x:output:0"adam/mul_56/ReadVariableOp:value:0*
T0*
_output_shapes	
:2
adam/mul_56c
adam/sub_37/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_37/xs
adam/sub_37Subadam/sub_37/x:output:0adam/Cast_47/x:output:0*
T0*
_output_shapes
: 2
adam/sub_37
adam/mul_57Muladam/sub_37:z:07clip_by_global_norm_1/clip_by_global_norm_1/_3:output:0*
T0*
_output_shapes	
:2
adam/mul_57
adam/mul_58Muladam/mul_57:z:07clip_by_global_norm_1/clip_by_global_norm_1/_3:output:0*
T0*
_output_shapes	
:2
adam/mul_58k
adam/add_28AddV2adam/mul_56:z:0adam/mul_58:z:0*
T0*
_output_shapes	
:2
adam/add_28m
adam/Pow_18Powadam/Cast_46/x:output:0adam/Cast_49:y:0*
T0*
_output_shapes
: 2
adam/Pow_18c
adam/sub_38/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_38/xk
adam/sub_38Subadam/sub_38/x:output:0adam/Pow_18:z:0*
T0*
_output_shapes
: 2
adam/sub_38u
adam/truediv_27RealDivadam/add_27:z:0adam/sub_38:z:0*
T0*
_output_shapes	
:2
adam/truediv_27m
adam/Pow_19Powadam/Cast_47/x:output:0adam/Cast_49:y:0*
T0*
_output_shapes
: 2
adam/Pow_19c
adam/sub_39/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_39/xk
adam/sub_39Subadam/sub_39/x:output:0adam/Pow_19:z:0*
T0*
_output_shapes
: 2
adam/sub_39u
adam/truediv_28RealDivadam/add_28:z:0adam/sub_39:z:0*
T0*
_output_shapes	
:2
adam/truediv_28u
adam/mul_59Muladam/Cast_45/x:output:0adam/truediv_27:z:0*
T0*
_output_shapes	
:2
adam/mul_59]
adam/Sqrt_9Sqrtadam/truediv_28:z:0*
T0*
_output_shapes	
:2
adam/Sqrt_9s
adam/add_29AddV2adam/Sqrt_9:y:0adam/Cast_48/x:output:0*
T0*
_output_shapes	
:2
adam/add_29u
adam/truediv_29RealDivadam/mul_59:z:0adam/add_29:z:0*
T0*
_output_shapes	
:2
adam/truediv_29ß
adam/AssignSubVariableOp_9AssignSubVariableOp9critic_sequential_dense_1_biasadd_readvariableop_resourceadam/truediv_29:z:01^critic/sequential/dense_1/BiasAdd/ReadVariableOp3^critic/sequential/dense_1/BiasAdd_1/ReadVariableOp3^critic/sequential/dense_1/BiasAdd_2/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_9¾
adam/AssignVariableOp_18AssignVariableOp#adam_mul_54_readvariableop_resourceadam/add_27:z:0^adam/mul_54/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_18¾
adam/AssignVariableOp_19AssignVariableOp#adam_mul_56_readvariableop_resourceadam/add_28:z:0^adam/mul_56/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_19e
adam/Cast_50/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_50/xe
adam/Cast_51/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_51/xe
adam/Cast_52/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_52/xe
adam/Cast_53/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_53/x³
adam/Cast_54/ReadVariableOpReadVariableOp#adam_assignaddvariableop_1_resource^adam/AssignAddVariableOp_1*
_output_shapes
: *
dtype0	2
adam/Cast_54/ReadVariableOpy
adam/Cast_54Cast#adam/Cast_54/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_54
adam/mul_60/ReadVariableOpReadVariableOp#adam_mul_60_readvariableop_resource*
_output_shapes
:	*
dtype02
adam/mul_60/ReadVariableOp
adam/mul_60Muladam/Cast_51/x:output:0"adam/mul_60/ReadVariableOp:value:0*
T0*
_output_shapes
:	2
adam/mul_60c
adam/sub_40/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_40/xs
adam/sub_40Subadam/sub_40/x:output:0adam/Cast_51/x:output:0*
T0*
_output_shapes
: 2
adam/sub_40
adam/mul_61Muladam/sub_40:z:07clip_by_global_norm_1/clip_by_global_norm_1/_4:output:0*
T0*
_output_shapes
:	2
adam/mul_61o
adam/add_30AddV2adam/mul_60:z:0adam/mul_61:z:0*
T0*
_output_shapes
:	2
adam/add_30
adam/mul_62/ReadVariableOpReadVariableOp#adam_mul_62_readvariableop_resource*
_output_shapes
:	*
dtype02
adam/mul_62/ReadVariableOp
adam/mul_62Muladam/Cast_52/x:output:0"adam/mul_62/ReadVariableOp:value:0*
T0*
_output_shapes
:	2
adam/mul_62c
adam/sub_41/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_41/xs
adam/sub_41Subadam/sub_41/x:output:0adam/Cast_52/x:output:0*
T0*
_output_shapes
: 2
adam/sub_41
adam/mul_63Muladam/sub_41:z:07clip_by_global_norm_1/clip_by_global_norm_1/_4:output:0*
T0*
_output_shapes
:	2
adam/mul_63
adam/mul_64Muladam/mul_63:z:07clip_by_global_norm_1/clip_by_global_norm_1/_4:output:0*
T0*
_output_shapes
:	2
adam/mul_64o
adam/add_31AddV2adam/mul_62:z:0adam/mul_64:z:0*
T0*
_output_shapes
:	2
adam/add_31m
adam/Pow_20Powadam/Cast_51/x:output:0adam/Cast_54:y:0*
T0*
_output_shapes
: 2
adam/Pow_20c
adam/sub_42/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_42/xk
adam/sub_42Subadam/sub_42/x:output:0adam/Pow_20:z:0*
T0*
_output_shapes
: 2
adam/sub_42y
adam/truediv_30RealDivadam/add_30:z:0adam/sub_42:z:0*
T0*
_output_shapes
:	2
adam/truediv_30m
adam/Pow_21Powadam/Cast_52/x:output:0adam/Cast_54:y:0*
T0*
_output_shapes
: 2
adam/Pow_21c
adam/sub_43/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_43/xk
adam/sub_43Subadam/sub_43/x:output:0adam/Pow_21:z:0*
T0*
_output_shapes
: 2
adam/sub_43y
adam/truediv_31RealDivadam/add_31:z:0adam/sub_43:z:0*
T0*
_output_shapes
:	2
adam/truediv_31y
adam/mul_65Muladam/Cast_50/x:output:0adam/truediv_30:z:0*
T0*
_output_shapes
:	2
adam/mul_65c
adam/Sqrt_10Sqrtadam/truediv_31:z:0*
T0*
_output_shapes
:	2
adam/Sqrt_10x
adam/add_32AddV2adam/Sqrt_10:y:0adam/Cast_53/x:output:0*
T0*
_output_shapes
:	2
adam/add_32y
adam/truediv_32RealDivadam/mul_65:z:0adam/add_32:z:0*
T0*
_output_shapes
:	2
adam/truediv_32Ý
adam/AssignSubVariableOp_10AssignSubVariableOp8critic_sequential_dense_2_matmul_readvariableop_resourceadam/truediv_32:z:00^critic/sequential/dense_2/MatMul/ReadVariableOp2^critic/sequential/dense_2/MatMul_1/ReadVariableOp2^critic/sequential/dense_2/MatMul_2/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_10¾
adam/AssignVariableOp_20AssignVariableOp#adam_mul_60_readvariableop_resourceadam/add_30:z:0^adam/mul_60/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_20¾
adam/AssignVariableOp_21AssignVariableOp#adam_mul_62_readvariableop_resourceadam/add_31:z:0^adam/mul_62/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_21e
adam/Cast_55/xConst*
_output_shapes
: *
dtype0*
valueB
 *RI92
adam/Cast_55/xe
adam/Cast_56/xConst*
_output_shapes
: *
dtype0*
valueB
 *fff?2
adam/Cast_56/xe
adam/Cast_57/xConst*
_output_shapes
: *
dtype0*
valueB
 *w¾?2
adam/Cast_57/xe
adam/Cast_58/xConst*
_output_shapes
: *
dtype0*
valueB
 *wÌ+22
adam/Cast_58/x³
adam/Cast_59/ReadVariableOpReadVariableOp#adam_assignaddvariableop_1_resource^adam/AssignAddVariableOp_1*
_output_shapes
: *
dtype0	2
adam/Cast_59/ReadVariableOpy
adam/Cast_59Cast#adam/Cast_59/ReadVariableOp:value:0*

DstT0*

SrcT0	*
_output_shapes
: 2
adam/Cast_59
adam/mul_66/ReadVariableOpReadVariableOp#adam_mul_66_readvariableop_resource*
_output_shapes
:*
dtype02
adam/mul_66/ReadVariableOp
adam/mul_66Muladam/Cast_56/x:output:0"adam/mul_66/ReadVariableOp:value:0*
T0*
_output_shapes
:2
adam/mul_66c
adam/sub_44/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_44/xs
adam/sub_44Subadam/sub_44/x:output:0adam/Cast_56/x:output:0*
T0*
_output_shapes
: 2
adam/sub_44
adam/mul_67Muladam/sub_44:z:07clip_by_global_norm_1/clip_by_global_norm_1/_5:output:0*
T0*
_output_shapes
:2
adam/mul_67j
adam/add_33AddV2adam/mul_66:z:0adam/mul_67:z:0*
T0*
_output_shapes
:2
adam/add_33
adam/mul_68/ReadVariableOpReadVariableOp#adam_mul_68_readvariableop_resource*
_output_shapes
:*
dtype02
adam/mul_68/ReadVariableOp
adam/mul_68Muladam/Cast_57/x:output:0"adam/mul_68/ReadVariableOp:value:0*
T0*
_output_shapes
:2
adam/mul_68c
adam/sub_45/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_45/xs
adam/sub_45Subadam/sub_45/x:output:0adam/Cast_57/x:output:0*
T0*
_output_shapes
: 2
adam/sub_45
adam/mul_69Muladam/sub_45:z:07clip_by_global_norm_1/clip_by_global_norm_1/_5:output:0*
T0*
_output_shapes
:2
adam/mul_69
adam/mul_70Muladam/mul_69:z:07clip_by_global_norm_1/clip_by_global_norm_1/_5:output:0*
T0*
_output_shapes
:2
adam/mul_70j
adam/add_34AddV2adam/mul_68:z:0adam/mul_70:z:0*
T0*
_output_shapes
:2
adam/add_34m
adam/Pow_22Powadam/Cast_56/x:output:0adam/Cast_59:y:0*
T0*
_output_shapes
: 2
adam/Pow_22c
adam/sub_46/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_46/xk
adam/sub_46Subadam/sub_46/x:output:0adam/Pow_22:z:0*
T0*
_output_shapes
: 2
adam/sub_46t
adam/truediv_33RealDivadam/add_33:z:0adam/sub_46:z:0*
T0*
_output_shapes
:2
adam/truediv_33m
adam/Pow_23Powadam/Cast_57/x:output:0adam/Cast_59:y:0*
T0*
_output_shapes
: 2
adam/Pow_23c
adam/sub_47/xConst*
_output_shapes
: *
dtype0*
valueB
 *  ?2
adam/sub_47/xk
adam/sub_47Subadam/sub_47/x:output:0adam/Pow_23:z:0*
T0*
_output_shapes
: 2
adam/sub_47t
adam/truediv_34RealDivadam/add_34:z:0adam/sub_47:z:0*
T0*
_output_shapes
:2
adam/truediv_34t
adam/mul_71Muladam/Cast_55/x:output:0adam/truediv_33:z:0*
T0*
_output_shapes
:2
adam/mul_71^
adam/Sqrt_11Sqrtadam/truediv_34:z:0*
T0*
_output_shapes
:2
adam/Sqrt_11s
adam/add_35AddV2adam/Sqrt_11:y:0adam/Cast_58/x:output:0*
T0*
_output_shapes
:2
adam/add_35t
adam/truediv_35RealDivadam/mul_71:z:0adam/add_35:z:0*
T0*
_output_shapes
:2
adam/truediv_35á
adam/AssignSubVariableOp_11AssignSubVariableOp9critic_sequential_dense_2_biasadd_readvariableop_resourceadam/truediv_35:z:01^critic/sequential/dense_2/BiasAdd/ReadVariableOp3^critic/sequential/dense_2/BiasAdd_1/ReadVariableOp3^critic/sequential/dense_2/BiasAdd_2/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignSubVariableOp_11¾
adam/AssignVariableOp_22AssignVariableOp#adam_mul_66_readvariableop_resourceadam/add_33:z:0^adam/mul_66/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_22¾
adam/AssignVariableOp_23AssignVariableOp#adam_mul_68_readvariableop_resourceadam/add_34:z:0^adam/mul_68/ReadVariableOp*
_output_shapes
 *
dtype02
adam/AssignVariableOp_23â
IdentityIdentityMean_1:output:0^adam/AssignAddVariableOp^adam/AssignAddVariableOp_1^adam/AssignSubVariableOp^adam/AssignSubVariableOp_1^adam/AssignSubVariableOp_10^adam/AssignSubVariableOp_11^adam/AssignSubVariableOp_2^adam/AssignSubVariableOp_3^adam/AssignSubVariableOp_4^adam/AssignSubVariableOp_5^adam/AssignSubVariableOp_6^adam/AssignSubVariableOp_7^adam/AssignSubVariableOp_8^adam/AssignSubVariableOp_9^adam/AssignVariableOp^adam/AssignVariableOp_1^adam/AssignVariableOp_10^adam/AssignVariableOp_11^adam/AssignVariableOp_12^adam/AssignVariableOp_13^adam/AssignVariableOp_14^adam/AssignVariableOp_15^adam/AssignVariableOp_16^adam/AssignVariableOp_17^adam/AssignVariableOp_18^adam/AssignVariableOp_19^adam/AssignVariableOp_2^adam/AssignVariableOp_20^adam/AssignVariableOp_21^adam/AssignVariableOp_22^adam/AssignVariableOp_23^adam/AssignVariableOp_3^adam/AssignVariableOp_4^adam/AssignVariableOp_5^adam/AssignVariableOp_6^adam/AssignVariableOp_7^adam/AssignVariableOp_8^adam/AssignVariableOp_9*
T0*
_output_shapes
: 2

Identityä

Identity_1IdentityMean:output:0^adam/AssignAddVariableOp^adam/AssignAddVariableOp_1^adam/AssignSubVariableOp^adam/AssignSubVariableOp_1^adam/AssignSubVariableOp_10^adam/AssignSubVariableOp_11^adam/AssignSubVariableOp_2^adam/AssignSubVariableOp_3^adam/AssignSubVariableOp_4^adam/AssignSubVariableOp_5^adam/AssignSubVariableOp_6^adam/AssignSubVariableOp_7^adam/AssignSubVariableOp_8^adam/AssignSubVariableOp_9^adam/AssignVariableOp^adam/AssignVariableOp_1^adam/AssignVariableOp_10^adam/AssignVariableOp_11^adam/AssignVariableOp_12^adam/AssignVariableOp_13^adam/AssignVariableOp_14^adam/AssignVariableOp_15^adam/AssignVariableOp_16^adam/AssignVariableOp_17^adam/AssignVariableOp_18^adam/AssignVariableOp_19^adam/AssignVariableOp_2^adam/AssignVariableOp_20^adam/AssignVariableOp_21^adam/AssignVariableOp_22^adam/AssignVariableOp_23^adam/AssignVariableOp_3^adam/AssignVariableOp_4^adam/AssignVariableOp_5^adam/AssignVariableOp_6^adam/AssignVariableOp_7^adam/AssignVariableOp_8^adam/AssignVariableOp_9*
T0*
_output_shapes
: 2

Identity_1"
identityIdentity:output:0"!

identity_1Identity_1:output:0*Ó
_input_shapesÁ
¾: : : : : ::::::::::::::::::::::::::::::::::::::24
adam/AssignAddVariableOpadam/AssignAddVariableOp28
adam/AssignAddVariableOp_1adam/AssignAddVariableOp_124
adam/AssignSubVariableOpadam/AssignSubVariableOp28
adam/AssignSubVariableOp_1adam/AssignSubVariableOp_12:
adam/AssignSubVariableOp_10adam/AssignSubVariableOp_102:
adam/AssignSubVariableOp_11adam/AssignSubVariableOp_1128
adam/AssignSubVariableOp_2adam/AssignSubVariableOp_228
adam/AssignSubVariableOp_3adam/AssignSubVariableOp_328
adam/AssignSubVariableOp_4adam/AssignSubVariableOp_428
adam/AssignSubVariableOp_5adam/AssignSubVariableOp_528
adam/AssignSubVariableOp_6adam/AssignSubVariableOp_628
adam/AssignSubVariableOp_7adam/AssignSubVariableOp_728
adam/AssignSubVariableOp_8adam/AssignSubVariableOp_828
adam/AssignSubVariableOp_9adam/AssignSubVariableOp_92.
adam/AssignVariableOpadam/AssignVariableOp22
adam/AssignVariableOp_1adam/AssignVariableOp_124
adam/AssignVariableOp_10adam/AssignVariableOp_1024
adam/AssignVariableOp_11adam/AssignVariableOp_1124
adam/AssignVariableOp_12adam/AssignVariableOp_1224
adam/AssignVariableOp_13adam/AssignVariableOp_1324
adam/AssignVariableOp_14adam/AssignVariableOp_1424
adam/AssignVariableOp_15adam/AssignVariableOp_1524
adam/AssignVariableOp_16adam/AssignVariableOp_1624
adam/AssignVariableOp_17adam/AssignVariableOp_1724
adam/AssignVariableOp_18adam/AssignVariableOp_1824
adam/AssignVariableOp_19adam/AssignVariableOp_1922
adam/AssignVariableOp_2adam/AssignVariableOp_224
adam/AssignVariableOp_20adam/AssignVariableOp_2024
adam/AssignVariableOp_21adam/AssignVariableOp_2124
adam/AssignVariableOp_22adam/AssignVariableOp_2224
adam/AssignVariableOp_23adam/AssignVariableOp_2322
adam/AssignVariableOp_3adam/AssignVariableOp_322
adam/AssignVariableOp_4adam/AssignVariableOp_422
adam/AssignVariableOp_5adam/AssignVariableOp_522
adam/AssignVariableOp_6adam/AssignVariableOp_622
adam/AssignVariableOp_7adam/AssignVariableOp_722
adam/AssignVariableOp_8adam/AssignVariableOp_822
adam/AssignVariableOp_9adam/AssignVariableOp_9:M I

_output_shapes

: 
'
_user_specified_nametransitions/0:IE

_output_shapes
: 
'
_user_specified_nametransitions/1:MI

_output_shapes

: 
'
_user_specified_nametransitions/2:IE

_output_shapes
: 
'
_user_specified_nametransitions/3:IE

_output_shapes
: 
'
_user_specified_nametransitions/4
Ü
|
'__inference_dense_1_layer_call_fn_13508

inputs
unknown
	unknown_0
identity¢StatefulPartitionedCalló
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_1_layer_call_and_return_conditional_losses_128232
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ::22
StatefulPartitionedCallStatefulPartitionedCall:P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs

ñ
G__inference_sequential_1_layer_call_and_return_conditional_losses_13434

inputs*
&dense_3_matmul_readvariableop_resource+
'dense_3_biasadd_readvariableop_resource*
&dense_4_matmul_readvariableop_resource+
'dense_4_biasadd_readvariableop_resource*
&dense_5_matmul_readvariableop_resource+
'dense_5_biasadd_readvariableop_resource
identity¦
dense_3/MatMul/ReadVariableOpReadVariableOp&dense_3_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_3/MatMul/ReadVariableOp
dense_3/MatMulMatMulinputs%dense_3/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/MatMul¥
dense_3/BiasAdd/ReadVariableOpReadVariableOp'dense_3_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_3/BiasAdd/ReadVariableOp¢
dense_3/BiasAddBiasAdddense_3/MatMul:product:0&dense_3/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/BiasAddq
dense_3/ReluReludense_3/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_3/Relu§
dense_4/MatMul/ReadVariableOpReadVariableOp&dense_4_matmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
dense_4/MatMul/ReadVariableOp 
dense_4/MatMulMatMuldense_3/Relu:activations:0%dense_4/MatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/MatMul¥
dense_4/BiasAdd/ReadVariableOpReadVariableOp'dense_4_biasadd_readvariableop_resource*
_output_shapes	
:*
dtype02 
dense_4/BiasAdd/ReadVariableOp¢
dense_4/BiasAddBiasAdddense_4/MatMul:product:0&dense_4/BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/BiasAddq
dense_4/ReluReludense_4/BiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_4/Relu¦
dense_5/MatMul/ReadVariableOpReadVariableOp&dense_5_matmul_readvariableop_resource*
_output_shapes
:	*
dtype02
dense_5/MatMul/ReadVariableOp
dense_5/MatMulMatMuldense_4/Relu:activations:0%dense_5/MatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/MatMul¤
dense_5/BiasAdd/ReadVariableOpReadVariableOp'dense_5_biasadd_readvariableop_resource*
_output_shapes
:*
dtype02 
dense_5/BiasAdd/ReadVariableOp¡
dense_5/BiasAddBiasAdddense_5/MatMul:product:0&dense_5/BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/BiasAddp
dense_5/TanhTanhdense_5/BiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
dense_5/Tanhd
IdentityIdentitydense_5/Tanh:y:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ:::::::O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
­
ª
B__inference_dense_3_layer_call_and_return_conditional_losses_12973

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource*
_output_shapes
:	*
dtype02
MatMul/ReadVariableOpt
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddY
ReluReluBiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Relug
IdentityIdentityRelu:activations:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*.
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
ã
½
,__inference_sequential_1_layer_call_fn_13468

inputs
unknown
	unknown_0
	unknown_1
	unknown_2
	unknown_3
	unknown_4
identity¢StatefulPartitionedCall«
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0	unknown_1	unknown_2	unknown_3	unknown_4*
Tin
	2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 *P
fKRI
G__inference_sequential_1_layer_call_and_return_conditional_losses_131212
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::22
StatefulPartitionedCallStatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
°
ª
B__inference_dense_1_layer_call_and_return_conditional_losses_13499

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
MatMul/ReadVariableOpt
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddY
ReluReluBiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Relug
IdentityIdentityRelu:activations:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
û
¶
E__inference_sequential_layer_call_and_return_conditional_losses_12943

inputs
dense_12927
dense_12929
dense_1_12932
dense_1_12934
dense_2_12937
dense_2_12939
identity¢dense/StatefulPartitionedCall¢dense_1/StatefulPartitionedCall¢dense_2/StatefulPartitionedCall
dense/StatefulPartitionedCallStatefulPartitionedCallinputsdense_12927dense_12929*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *I
fDRB
@__inference_dense_layer_call_and_return_conditional_losses_127962
dense/StatefulPartitionedCall­
dense_1/StatefulPartitionedCallStatefulPartitionedCall&dense/StatefulPartitionedCall:output:0dense_1_12932dense_1_12934*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_1_layer_call_and_return_conditional_losses_128232!
dense_1/StatefulPartitionedCall®
dense_2/StatefulPartitionedCallStatefulPartitionedCall(dense_1/StatefulPartitionedCall:output:0dense_2_12937dense_2_12939*
Tin
2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_2_layer_call_and_return_conditional_losses_128492!
dense_2/StatefulPartitionedCallà
IdentityIdentity(dense_2/StatefulPartitionedCall:output:0^dense/StatefulPartitionedCall ^dense_1/StatefulPartitionedCall ^dense_2/StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::2>
dense/StatefulPartitionedCalldense/StatefulPartitionedCall2B
dense_1/StatefulPartitionedCalldense_1/StatefulPartitionedCall2B
dense_2/StatefulPartitionedCalldense_2/StatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
­
ª
B__inference_dense_3_layer_call_and_return_conditional_losses_13538

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource*
_output_shapes
:	*
dtype02
MatMul/ReadVariableOpt
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddY
ReluReluBiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Relug
IdentityIdentityRelu:activations:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*.
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
»
µ
#__inference_signature_wrapper_12756
asksize
	askvector
bidsize
	bidvector
contractholdings
finalfundamentalestimate
latency	
marketholdings
numtransactions
omegaratioask
omegaratiobid

privateask

privatebid
side

spread
timesincelasttrade	

timetilend	
transactionhistory
unknown
	unknown_0
	unknown_1
	unknown_2
	unknown_3
	unknown_4
	unknown_5
identity

identity_1

identity_2

identity_3

identity_4¢StatefulPartitionedCall
StatefulPartitionedCallStatefulPartitionedCallfinalfundamentalestimate
privatebid
privateaskomegaratiobidomegaratioasksidebidsizeasksizespreadmarketholdingscontractholdingsnumtransactions
timetilendlatencytimesincelasttrade	bidvector	askvectortransactionhistoryunknown	unknown_0	unknown_1	unknown_2	unknown_3	unknown_4	unknown_5*$
Tin
2			*
Tout	
2*
_collective_manager_ids
 *"
_output_shapes
:: : :: *(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 * 
fR
__inference__policy_5132
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*
_output_shapes
:2

Identity

Identity_1Identity StatefulPartitionedCall:output:1^StatefulPartitionedCall*
T0*
_output_shapes
: 2

Identity_1

Identity_2Identity StatefulPartitionedCall:output:2^StatefulPartitionedCall*
T0*
_output_shapes
: 2

Identity_2

Identity_3Identity StatefulPartitionedCall:output:3^StatefulPartitionedCall*
T0*
_output_shapes
:2

Identity_3

Identity_4Identity StatefulPartitionedCall:output:4^StatefulPartitionedCall*
T0*
_output_shapes
: 2

Identity_4"
identityIdentity:output:0"!

identity_1Identity_1:output:0"!

identity_2Identity_2:output:0"!

identity_3Identity_3:output:0"!

identity_4Identity_4:output:0*z
_input_shapesi
g: :ÿÿÿÿÿÿÿÿÿ: :ÿÿÿÿÿÿÿÿÿ: : : : : : : : : : : : : :ÿÿÿÿÿÿÿÿÿ:::::::22
StatefulPartitionedCallStatefulPartitionedCall:? ;

_output_shapes
: 
!
_user_specified_name	askSize:NJ
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
#
_user_specified_name	askVector:?;

_output_shapes
: 
!
_user_specified_name	bidSize:NJ
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
#
_user_specified_name	bidVector:HD

_output_shapes
: 
*
_user_specified_namecontractHoldings:PL

_output_shapes
: 
2
_user_specified_namefinalFundamentalEstimate:?;

_output_shapes
: 
!
_user_specified_name	latency:FB

_output_shapes
: 
(
_user_specified_namemarketHoldings:GC

_output_shapes
: 
)
_user_specified_namenumTransactions:E	A

_output_shapes
: 
'
_user_specified_nameomegaRatioAsk:E
A

_output_shapes
: 
'
_user_specified_nameomegaRatioBid:B>

_output_shapes
: 
$
_user_specified_name
privateAsk:B>

_output_shapes
: 
$
_user_specified_name
privateBid:<8

_output_shapes
: 

_user_specified_nameside:>:

_output_shapes
: 
 
_user_specified_namespread:JF

_output_shapes
: 
,
_user_specified_nametimeSinceLastTrade:B>

_output_shapes
: 
$
_user_specified_name
timeTilEnd:WS
#
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
,
_user_specified_nametransactionHistory
Ö
z
%__inference_dense_layer_call_fn_13488

inputs
unknown
	unknown_0
identity¢StatefulPartitionedCallñ
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *I
fDRB
@__inference_dense_layer_call_and_return_conditional_losses_127962
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*.
_input_shapes
:ÿÿÿÿÿÿÿÿÿ::22
StatefulPartitionedCallStatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
çP

__inference__traced_save_13748
file_prefix'
#savev2_variable_read_readvariableop%
!savev2_adam_t_read_readvariableop	'
#savev2_adam_t_1_read_readvariableop	4
0savev2_adam_m_1_dense_kernel_read_readvariableop2
.savev2_adam_m_1_dense_bias_read_readvariableop6
2savev2_adam_m_1_dense_1_kernel_read_readvariableop4
0savev2_adam_m_1_dense_1_bias_read_readvariableop6
2savev2_adam_m_1_dense_2_kernel_read_readvariableop4
0savev2_adam_m_1_dense_2_bias_read_readvariableop4
0savev2_adam_v_1_dense_kernel_read_readvariableop2
.savev2_adam_v_1_dense_bias_read_readvariableop6
2savev2_adam_v_1_dense_1_kernel_read_readvariableop4
0savev2_adam_v_1_dense_1_bias_read_readvariableop6
2savev2_adam_v_1_dense_2_kernel_read_readvariableop4
0savev2_adam_v_1_dense_2_bias_read_readvariableop4
0savev2_adam_m_dense_3_kernel_read_readvariableop2
.savev2_adam_m_dense_3_bias_read_readvariableop4
0savev2_adam_m_dense_4_kernel_read_readvariableop2
.savev2_adam_m_dense_4_bias_read_readvariableop4
0savev2_adam_m_dense_5_kernel_read_readvariableop2
.savev2_adam_m_dense_5_bias_read_readvariableop4
0savev2_adam_v_dense_3_kernel_read_readvariableop2
.savev2_adam_v_dense_3_bias_read_readvariableop4
0savev2_adam_v_dense_4_kernel_read_readvariableop2
.savev2_adam_v_dense_4_bias_read_readvariableop4
0savev2_adam_v_dense_5_kernel_read_readvariableop2
.savev2_adam_v_dense_5_bias_read_readvariableop+
'savev2_dense_kernel_read_readvariableop)
%savev2_dense_bias_read_readvariableop-
)savev2_dense_1_kernel_read_readvariableop+
'savev2_dense_1_bias_read_readvariableop-
)savev2_dense_2_kernel_read_readvariableop+
'savev2_dense_2_bias_read_readvariableop-
)savev2_dense_3_kernel_read_readvariableop+
'savev2_dense_3_bias_read_readvariableop-
)savev2_dense_4_kernel_read_readvariableop+
'savev2_dense_4_bias_read_readvariableop-
)savev2_dense_5_kernel_read_readvariableop+
'savev2_dense_5_bias_read_readvariableop
savev2_const

identity_1¢MergeV2Checkpoints
StaticRegexFullMatchStaticRegexFullMatchfile_prefix"/device:CPU:**
_output_shapes
: *
pattern
^s3://.*2
StaticRegexFullMatchc
ConstConst"/device:CPU:**
_output_shapes
: *
dtype0*
valueB B.part2
Const
Const_1Const"/device:CPU:**
_output_shapes
: *
dtype0*<
value3B1 B+_temp_8dc184a526854a2c83956cb4b13e3a02/part2	
Const_1
SelectSelectStaticRegexFullMatch:output:0Const:output:0Const_1:output:0"/device:CPU:**
T0*
_output_shapes
: 2
Selectt

StringJoin
StringJoinfile_prefixSelect:output:0"/device:CPU:**
N*
_output_shapes
: 2

StringJoinZ

num_shardsConst*
_output_shapes
: *
dtype0*
value	B :2

num_shards
ShardedFilename/shardConst"/device:CPU:0*
_output_shapes
: *
dtype0*
value	B : 2
ShardedFilename/shard¦
ShardedFilenameShardedFilenameStringJoin:output:0ShardedFilename/shard:output:0num_shards:output:0"/device:CPU:0*
_output_shapes
: 2
ShardedFilename
SaveV2/tensor_namesConst"/device:CPU:0*
_output_shapes
:(*
dtype0*¢
valueB(B&buffer_size/.ATTRIBUTES/VARIABLE_VALUEB0critic_optimizer/step/.ATTRIBUTES/VARIABLE_VALUEB/actor_optimizer/step/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/0/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/1/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/2/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/3/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/4/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/5/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/0/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/1/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/2/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/3/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/4/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/5/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/0/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/1/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/2/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/3/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/4/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/5/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/0/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/1/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/2/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/3/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/4/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/5/.ATTRIBUTES/VARIABLE_VALUEBRonline_critic/critic_layers/layer_with_weights-0/kernel/.ATTRIBUTES/VARIABLE_VALUEBPonline_critic/critic_layers/layer_with_weights-0/bias/.ATTRIBUTES/VARIABLE_VALUEBRonline_critic/critic_layers/layer_with_weights-1/kernel/.ATTRIBUTES/VARIABLE_VALUEBPonline_critic/critic_layers/layer_with_weights-1/bias/.ATTRIBUTES/VARIABLE_VALUEBRonline_critic/critic_layers/layer_with_weights-2/kernel/.ATTRIBUTES/VARIABLE_VALUEBPonline_critic/critic_layers/layer_with_weights-2/bias/.ATTRIBUTES/VARIABLE_VALUEBPonline_actor/actor_layers/layer_with_weights-0/kernel/.ATTRIBUTES/VARIABLE_VALUEBNonline_actor/actor_layers/layer_with_weights-0/bias/.ATTRIBUTES/VARIABLE_VALUEBPonline_actor/actor_layers/layer_with_weights-1/kernel/.ATTRIBUTES/VARIABLE_VALUEBNonline_actor/actor_layers/layer_with_weights-1/bias/.ATTRIBUTES/VARIABLE_VALUEBPonline_actor/actor_layers/layer_with_weights-2/kernel/.ATTRIBUTES/VARIABLE_VALUEBNonline_actor/actor_layers/layer_with_weights-2/bias/.ATTRIBUTES/VARIABLE_VALUEB_CHECKPOINTABLE_OBJECT_GRAPH2
SaveV2/tensor_namesØ
SaveV2/shape_and_slicesConst"/device:CPU:0*
_output_shapes
:(*
dtype0*c
valueZBX(B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B 2
SaveV2/shape_and_slicesê
SaveV2SaveV2ShardedFilename:filename:0SaveV2/tensor_names:output:0 SaveV2/shape_and_slices:output:0#savev2_variable_read_readvariableop!savev2_adam_t_read_readvariableop#savev2_adam_t_1_read_readvariableop0savev2_adam_m_1_dense_kernel_read_readvariableop.savev2_adam_m_1_dense_bias_read_readvariableop2savev2_adam_m_1_dense_1_kernel_read_readvariableop0savev2_adam_m_1_dense_1_bias_read_readvariableop2savev2_adam_m_1_dense_2_kernel_read_readvariableop0savev2_adam_m_1_dense_2_bias_read_readvariableop0savev2_adam_v_1_dense_kernel_read_readvariableop.savev2_adam_v_1_dense_bias_read_readvariableop2savev2_adam_v_1_dense_1_kernel_read_readvariableop0savev2_adam_v_1_dense_1_bias_read_readvariableop2savev2_adam_v_1_dense_2_kernel_read_readvariableop0savev2_adam_v_1_dense_2_bias_read_readvariableop0savev2_adam_m_dense_3_kernel_read_readvariableop.savev2_adam_m_dense_3_bias_read_readvariableop0savev2_adam_m_dense_4_kernel_read_readvariableop.savev2_adam_m_dense_4_bias_read_readvariableop0savev2_adam_m_dense_5_kernel_read_readvariableop.savev2_adam_m_dense_5_bias_read_readvariableop0savev2_adam_v_dense_3_kernel_read_readvariableop.savev2_adam_v_dense_3_bias_read_readvariableop0savev2_adam_v_dense_4_kernel_read_readvariableop.savev2_adam_v_dense_4_bias_read_readvariableop0savev2_adam_v_dense_5_kernel_read_readvariableop.savev2_adam_v_dense_5_bias_read_readvariableop'savev2_dense_kernel_read_readvariableop%savev2_dense_bias_read_readvariableop)savev2_dense_1_kernel_read_readvariableop'savev2_dense_1_bias_read_readvariableop)savev2_dense_2_kernel_read_readvariableop'savev2_dense_2_bias_read_readvariableop)savev2_dense_3_kernel_read_readvariableop'savev2_dense_3_bias_read_readvariableop)savev2_dense_4_kernel_read_readvariableop'savev2_dense_4_bias_read_readvariableop)savev2_dense_5_kernel_read_readvariableop'savev2_dense_5_bias_read_readvariableopsavev2_const"/device:CPU:0*
_output_shapes
 *6
dtypes,
*2(		2
SaveV2º
&MergeV2Checkpoints/checkpoint_prefixesPackShardedFilename:filename:0^SaveV2"/device:CPU:0*
N*
T0*
_output_shapes
:2(
&MergeV2Checkpoints/checkpoint_prefixes¡
MergeV2CheckpointsMergeV2Checkpoints/MergeV2Checkpoints/checkpoint_prefixes:output:0file_prefix"/device:CPU:0*
_output_shapes
 2
MergeV2Checkpointsr
IdentityIdentityfile_prefix^MergeV2Checkpoints"/device:CPU:0*
T0*
_output_shapes
: 2

Identitym

Identity_1IdentityIdentity:output:0^MergeV2Checkpoints*
T0*
_output_shapes
: 2

Identity_1"!

identity_1Identity_1:output:0*ã
_input_shapesÑ
Î: : : : :	::
::	::	::
::	::	::
::	::	::
::	::	::
::	::	::
::	:: 2(
MergeV2CheckpointsMergeV2Checkpoints:C ?

_output_shapes
: 
%
_user_specified_namefile_prefix:

_output_shapes
: :

_output_shapes
: :

_output_shapes
: :%!

_output_shapes
:	:!

_output_shapes	
::&"
 
_output_shapes
:
:!

_output_shapes	
::%!

_output_shapes
:	: 	

_output_shapes
::%
!

_output_shapes
:	:!

_output_shapes	
::&"
 
_output_shapes
:
:!

_output_shapes	
::%!

_output_shapes
:	: 

_output_shapes
::%!

_output_shapes
:	:!

_output_shapes	
::&"
 
_output_shapes
:
:!

_output_shapes	
::%!

_output_shapes
:	: 

_output_shapes
::%!

_output_shapes
:	:!

_output_shapes	
::&"
 
_output_shapes
:
:!

_output_shapes	
::%!

_output_shapes
:	: 

_output_shapes
::%!

_output_shapes
:	:!

_output_shapes	
::&"
 
_output_shapes
:
:!

_output_shapes	
::% !

_output_shapes
:	: !

_output_shapes
::%"!

_output_shapes
:	:!#

_output_shapes	
::&$"
 
_output_shapes
:
:!%

_output_shapes	
::%&!

_output_shapes
:	: '

_output_shapes
::(

_output_shapes
: 
Ð
_
cond_1_false_480
cond_1_placeholder
cond_1_identity_privateask
cond_1_identityk
cond_1/IdentityIdentitycond_1_identity_privateask*
T0*
_output_shapes
: 2
cond_1/Identity"+
cond_1_identitycond_1/Identity:output:0*
_input_shapes
: : : 

_output_shapes
: :

_output_shapes
: 
Î
ª
B__inference_dense_2_layer_call_and_return_conditional_losses_13518

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource*
_output_shapes
:	*
dtype02
MatMul/ReadVariableOps
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddd
IdentityIdentityBiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
 
ª
B__inference_dense_5_layer_call_and_return_conditional_losses_13027

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource*
_output_shapes
:	*
dtype02
MatMul/ReadVariableOps
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddX
TanhTanhBiasAdd:output:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Tanh\
IdentityIdentityTanh:y:0*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
ã
½
,__inference_sequential_1_layer_call_fn_13451

inputs
unknown
	unknown_0
	unknown_1
	unknown_2
	unknown_3
	unknown_4
identity¢StatefulPartitionedCall«
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0	unknown_1	unknown_2	unknown_3	unknown_4*
Tin
	2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 *P
fKRI
G__inference_sequential_1_layer_call_and_return_conditional_losses_130852
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::22
StatefulPartitionedCallStatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
Ü
|
'__inference_dense_4_layer_call_fn_13567

inputs
unknown
	unknown_0
identity¢StatefulPartitionedCalló
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0*
Tin
2*
Tout
2*
_collective_manager_ids
 *(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_4_layer_call_and_return_conditional_losses_130002
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ::22
StatefulPartitionedCallStatefulPartitionedCall:P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
î
À
*__inference_sequential_layer_call_fn_13283
dense_input
unknown
	unknown_0
	unknown_1
	unknown_2
	unknown_3
	unknown_4
identity¢StatefulPartitionedCall®
StatefulPartitionedCallStatefulPartitionedCalldense_inputunknown	unknown_0	unknown_1	unknown_2	unknown_3	unknown_4*
Tin
	2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 *N
fIRG
E__inference_sequential_layer_call_and_return_conditional_losses_129072
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::22
StatefulPartitionedCallStatefulPartitionedCall:T P
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
%
_user_specified_namedense_input
Ú
|
'__inference_dense_2_layer_call_fn_13527

inputs
unknown
	unknown_0
identity¢StatefulPartitionedCallò
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0*
Tin
2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*$
_read_only_resource_inputs
*-
config_proto

CPU

GPU 2J 8 *K
fFRD
B__inference_dense_2_layer_call_and_return_conditional_losses_128492
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ::22
StatefulPartitionedCallStatefulPartitionedCall:P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
ß
»
*__inference_sequential_layer_call_fn_13218

inputs
unknown
	unknown_0
	unknown_1
	unknown_2
	unknown_3
	unknown_4
identity¢StatefulPartitionedCall©
StatefulPartitionedCallStatefulPartitionedCallinputsunknown	unknown_0	unknown_1	unknown_2	unknown_3	unknown_4*
Tin
	2*
Tout
2*
_collective_manager_ids
 *'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ*(
_read_only_resource_inputs

*-
config_proto

CPU

GPU 2J 8 *N
fIRG
E__inference_sequential_layer_call_and_return_conditional_losses_129432
StatefulPartitionedCall
IdentityIdentity StatefulPartitionedCall:output:0^StatefulPartitionedCall*
T0*'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*>
_input_shapes-
+:ÿÿÿÿÿÿÿÿÿ::::::22
StatefulPartitionedCallStatefulPartitionedCall:O K
'
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs
¢

!__inference__traced_restore_13875
file_prefix
assignvariableop_variable
assignvariableop_1_adam_t
assignvariableop_2_adam_t_1,
(assignvariableop_3_adam_m_1_dense_kernel*
&assignvariableop_4_adam_m_1_dense_bias.
*assignvariableop_5_adam_m_1_dense_1_kernel,
(assignvariableop_6_adam_m_1_dense_1_bias.
*assignvariableop_7_adam_m_1_dense_2_kernel,
(assignvariableop_8_adam_m_1_dense_2_bias,
(assignvariableop_9_adam_v_1_dense_kernel+
'assignvariableop_10_adam_v_1_dense_bias/
+assignvariableop_11_adam_v_1_dense_1_kernel-
)assignvariableop_12_adam_v_1_dense_1_bias/
+assignvariableop_13_adam_v_1_dense_2_kernel-
)assignvariableop_14_adam_v_1_dense_2_bias-
)assignvariableop_15_adam_m_dense_3_kernel+
'assignvariableop_16_adam_m_dense_3_bias-
)assignvariableop_17_adam_m_dense_4_kernel+
'assignvariableop_18_adam_m_dense_4_bias-
)assignvariableop_19_adam_m_dense_5_kernel+
'assignvariableop_20_adam_m_dense_5_bias-
)assignvariableop_21_adam_v_dense_3_kernel+
'assignvariableop_22_adam_v_dense_3_bias-
)assignvariableop_23_adam_v_dense_4_kernel+
'assignvariableop_24_adam_v_dense_4_bias-
)assignvariableop_25_adam_v_dense_5_kernel+
'assignvariableop_26_adam_v_dense_5_bias$
 assignvariableop_27_dense_kernel"
assignvariableop_28_dense_bias&
"assignvariableop_29_dense_1_kernel$
 assignvariableop_30_dense_1_bias&
"assignvariableop_31_dense_2_kernel$
 assignvariableop_32_dense_2_bias&
"assignvariableop_33_dense_3_kernel$
 assignvariableop_34_dense_3_bias&
"assignvariableop_35_dense_4_kernel$
 assignvariableop_36_dense_4_bias&
"assignvariableop_37_dense_5_kernel$
 assignvariableop_38_dense_5_bias
identity_40¢AssignVariableOp¢AssignVariableOp_1¢AssignVariableOp_10¢AssignVariableOp_11¢AssignVariableOp_12¢AssignVariableOp_13¢AssignVariableOp_14¢AssignVariableOp_15¢AssignVariableOp_16¢AssignVariableOp_17¢AssignVariableOp_18¢AssignVariableOp_19¢AssignVariableOp_2¢AssignVariableOp_20¢AssignVariableOp_21¢AssignVariableOp_22¢AssignVariableOp_23¢AssignVariableOp_24¢AssignVariableOp_25¢AssignVariableOp_26¢AssignVariableOp_27¢AssignVariableOp_28¢AssignVariableOp_29¢AssignVariableOp_3¢AssignVariableOp_30¢AssignVariableOp_31¢AssignVariableOp_32¢AssignVariableOp_33¢AssignVariableOp_34¢AssignVariableOp_35¢AssignVariableOp_36¢AssignVariableOp_37¢AssignVariableOp_38¢AssignVariableOp_4¢AssignVariableOp_5¢AssignVariableOp_6¢AssignVariableOp_7¢AssignVariableOp_8¢AssignVariableOp_9
RestoreV2/tensor_namesConst"/device:CPU:0*
_output_shapes
:(*
dtype0*¢
valueB(B&buffer_size/.ATTRIBUTES/VARIABLE_VALUEB0critic_optimizer/step/.ATTRIBUTES/VARIABLE_VALUEB/actor_optimizer/step/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/0/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/1/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/2/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/3/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/4/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/m/5/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/0/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/1/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/2/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/3/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/4/.ATTRIBUTES/VARIABLE_VALUEB/critic_optimizer/v/5/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/0/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/1/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/2/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/3/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/4/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/m/5/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/0/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/1/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/2/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/3/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/4/.ATTRIBUTES/VARIABLE_VALUEB.actor_optimizer/v/5/.ATTRIBUTES/VARIABLE_VALUEBRonline_critic/critic_layers/layer_with_weights-0/kernel/.ATTRIBUTES/VARIABLE_VALUEBPonline_critic/critic_layers/layer_with_weights-0/bias/.ATTRIBUTES/VARIABLE_VALUEBRonline_critic/critic_layers/layer_with_weights-1/kernel/.ATTRIBUTES/VARIABLE_VALUEBPonline_critic/critic_layers/layer_with_weights-1/bias/.ATTRIBUTES/VARIABLE_VALUEBRonline_critic/critic_layers/layer_with_weights-2/kernel/.ATTRIBUTES/VARIABLE_VALUEBPonline_critic/critic_layers/layer_with_weights-2/bias/.ATTRIBUTES/VARIABLE_VALUEBPonline_actor/actor_layers/layer_with_weights-0/kernel/.ATTRIBUTES/VARIABLE_VALUEBNonline_actor/actor_layers/layer_with_weights-0/bias/.ATTRIBUTES/VARIABLE_VALUEBPonline_actor/actor_layers/layer_with_weights-1/kernel/.ATTRIBUTES/VARIABLE_VALUEBNonline_actor/actor_layers/layer_with_weights-1/bias/.ATTRIBUTES/VARIABLE_VALUEBPonline_actor/actor_layers/layer_with_weights-2/kernel/.ATTRIBUTES/VARIABLE_VALUEBNonline_actor/actor_layers/layer_with_weights-2/bias/.ATTRIBUTES/VARIABLE_VALUEB_CHECKPOINTABLE_OBJECT_GRAPH2
RestoreV2/tensor_namesÞ
RestoreV2/shape_and_slicesConst"/device:CPU:0*
_output_shapes
:(*
dtype0*c
valueZBX(B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B 2
RestoreV2/shape_and_slicesö
	RestoreV2	RestoreV2file_prefixRestoreV2/tensor_names:output:0#RestoreV2/shape_and_slices:output:0"/device:CPU:0*¶
_output_shapes£
 ::::::::::::::::::::::::::::::::::::::::*6
dtypes,
*2(		2
	RestoreV2g
IdentityIdentityRestoreV2:tensors:0"/device:CPU:0*
T0*
_output_shapes
:2

Identity
AssignVariableOpAssignVariableOpassignvariableop_variableIdentity:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOpk

Identity_1IdentityRestoreV2:tensors:1"/device:CPU:0*
T0	*
_output_shapes
:2

Identity_1
AssignVariableOp_1AssignVariableOpassignvariableop_1_adam_tIdentity_1:output:0"/device:CPU:0*
_output_shapes
 *
dtype0	2
AssignVariableOp_1k

Identity_2IdentityRestoreV2:tensors:2"/device:CPU:0*
T0	*
_output_shapes
:2

Identity_2 
AssignVariableOp_2AssignVariableOpassignvariableop_2_adam_t_1Identity_2:output:0"/device:CPU:0*
_output_shapes
 *
dtype0	2
AssignVariableOp_2k

Identity_3IdentityRestoreV2:tensors:3"/device:CPU:0*
T0*
_output_shapes
:2

Identity_3­
AssignVariableOp_3AssignVariableOp(assignvariableop_3_adam_m_1_dense_kernelIdentity_3:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_3k

Identity_4IdentityRestoreV2:tensors:4"/device:CPU:0*
T0*
_output_shapes
:2

Identity_4«
AssignVariableOp_4AssignVariableOp&assignvariableop_4_adam_m_1_dense_biasIdentity_4:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_4k

Identity_5IdentityRestoreV2:tensors:5"/device:CPU:0*
T0*
_output_shapes
:2

Identity_5¯
AssignVariableOp_5AssignVariableOp*assignvariableop_5_adam_m_1_dense_1_kernelIdentity_5:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_5k

Identity_6IdentityRestoreV2:tensors:6"/device:CPU:0*
T0*
_output_shapes
:2

Identity_6­
AssignVariableOp_6AssignVariableOp(assignvariableop_6_adam_m_1_dense_1_biasIdentity_6:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_6k

Identity_7IdentityRestoreV2:tensors:7"/device:CPU:0*
T0*
_output_shapes
:2

Identity_7¯
AssignVariableOp_7AssignVariableOp*assignvariableop_7_adam_m_1_dense_2_kernelIdentity_7:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_7k

Identity_8IdentityRestoreV2:tensors:8"/device:CPU:0*
T0*
_output_shapes
:2

Identity_8­
AssignVariableOp_8AssignVariableOp(assignvariableop_8_adam_m_1_dense_2_biasIdentity_8:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_8k

Identity_9IdentityRestoreV2:tensors:9"/device:CPU:0*
T0*
_output_shapes
:2

Identity_9­
AssignVariableOp_9AssignVariableOp(assignvariableop_9_adam_v_1_dense_kernelIdentity_9:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_9n
Identity_10IdentityRestoreV2:tensors:10"/device:CPU:0*
T0*
_output_shapes
:2
Identity_10¯
AssignVariableOp_10AssignVariableOp'assignvariableop_10_adam_v_1_dense_biasIdentity_10:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_10n
Identity_11IdentityRestoreV2:tensors:11"/device:CPU:0*
T0*
_output_shapes
:2
Identity_11³
AssignVariableOp_11AssignVariableOp+assignvariableop_11_adam_v_1_dense_1_kernelIdentity_11:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_11n
Identity_12IdentityRestoreV2:tensors:12"/device:CPU:0*
T0*
_output_shapes
:2
Identity_12±
AssignVariableOp_12AssignVariableOp)assignvariableop_12_adam_v_1_dense_1_biasIdentity_12:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_12n
Identity_13IdentityRestoreV2:tensors:13"/device:CPU:0*
T0*
_output_shapes
:2
Identity_13³
AssignVariableOp_13AssignVariableOp+assignvariableop_13_adam_v_1_dense_2_kernelIdentity_13:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_13n
Identity_14IdentityRestoreV2:tensors:14"/device:CPU:0*
T0*
_output_shapes
:2
Identity_14±
AssignVariableOp_14AssignVariableOp)assignvariableop_14_adam_v_1_dense_2_biasIdentity_14:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_14n
Identity_15IdentityRestoreV2:tensors:15"/device:CPU:0*
T0*
_output_shapes
:2
Identity_15±
AssignVariableOp_15AssignVariableOp)assignvariableop_15_adam_m_dense_3_kernelIdentity_15:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_15n
Identity_16IdentityRestoreV2:tensors:16"/device:CPU:0*
T0*
_output_shapes
:2
Identity_16¯
AssignVariableOp_16AssignVariableOp'assignvariableop_16_adam_m_dense_3_biasIdentity_16:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_16n
Identity_17IdentityRestoreV2:tensors:17"/device:CPU:0*
T0*
_output_shapes
:2
Identity_17±
AssignVariableOp_17AssignVariableOp)assignvariableop_17_adam_m_dense_4_kernelIdentity_17:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_17n
Identity_18IdentityRestoreV2:tensors:18"/device:CPU:0*
T0*
_output_shapes
:2
Identity_18¯
AssignVariableOp_18AssignVariableOp'assignvariableop_18_adam_m_dense_4_biasIdentity_18:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_18n
Identity_19IdentityRestoreV2:tensors:19"/device:CPU:0*
T0*
_output_shapes
:2
Identity_19±
AssignVariableOp_19AssignVariableOp)assignvariableop_19_adam_m_dense_5_kernelIdentity_19:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_19n
Identity_20IdentityRestoreV2:tensors:20"/device:CPU:0*
T0*
_output_shapes
:2
Identity_20¯
AssignVariableOp_20AssignVariableOp'assignvariableop_20_adam_m_dense_5_biasIdentity_20:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_20n
Identity_21IdentityRestoreV2:tensors:21"/device:CPU:0*
T0*
_output_shapes
:2
Identity_21±
AssignVariableOp_21AssignVariableOp)assignvariableop_21_adam_v_dense_3_kernelIdentity_21:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_21n
Identity_22IdentityRestoreV2:tensors:22"/device:CPU:0*
T0*
_output_shapes
:2
Identity_22¯
AssignVariableOp_22AssignVariableOp'assignvariableop_22_adam_v_dense_3_biasIdentity_22:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_22n
Identity_23IdentityRestoreV2:tensors:23"/device:CPU:0*
T0*
_output_shapes
:2
Identity_23±
AssignVariableOp_23AssignVariableOp)assignvariableop_23_adam_v_dense_4_kernelIdentity_23:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_23n
Identity_24IdentityRestoreV2:tensors:24"/device:CPU:0*
T0*
_output_shapes
:2
Identity_24¯
AssignVariableOp_24AssignVariableOp'assignvariableop_24_adam_v_dense_4_biasIdentity_24:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_24n
Identity_25IdentityRestoreV2:tensors:25"/device:CPU:0*
T0*
_output_shapes
:2
Identity_25±
AssignVariableOp_25AssignVariableOp)assignvariableop_25_adam_v_dense_5_kernelIdentity_25:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_25n
Identity_26IdentityRestoreV2:tensors:26"/device:CPU:0*
T0*
_output_shapes
:2
Identity_26¯
AssignVariableOp_26AssignVariableOp'assignvariableop_26_adam_v_dense_5_biasIdentity_26:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_26n
Identity_27IdentityRestoreV2:tensors:27"/device:CPU:0*
T0*
_output_shapes
:2
Identity_27¨
AssignVariableOp_27AssignVariableOp assignvariableop_27_dense_kernelIdentity_27:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_27n
Identity_28IdentityRestoreV2:tensors:28"/device:CPU:0*
T0*
_output_shapes
:2
Identity_28¦
AssignVariableOp_28AssignVariableOpassignvariableop_28_dense_biasIdentity_28:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_28n
Identity_29IdentityRestoreV2:tensors:29"/device:CPU:0*
T0*
_output_shapes
:2
Identity_29ª
AssignVariableOp_29AssignVariableOp"assignvariableop_29_dense_1_kernelIdentity_29:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_29n
Identity_30IdentityRestoreV2:tensors:30"/device:CPU:0*
T0*
_output_shapes
:2
Identity_30¨
AssignVariableOp_30AssignVariableOp assignvariableop_30_dense_1_biasIdentity_30:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_30n
Identity_31IdentityRestoreV2:tensors:31"/device:CPU:0*
T0*
_output_shapes
:2
Identity_31ª
AssignVariableOp_31AssignVariableOp"assignvariableop_31_dense_2_kernelIdentity_31:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_31n
Identity_32IdentityRestoreV2:tensors:32"/device:CPU:0*
T0*
_output_shapes
:2
Identity_32¨
AssignVariableOp_32AssignVariableOp assignvariableop_32_dense_2_biasIdentity_32:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_32n
Identity_33IdentityRestoreV2:tensors:33"/device:CPU:0*
T0*
_output_shapes
:2
Identity_33ª
AssignVariableOp_33AssignVariableOp"assignvariableop_33_dense_3_kernelIdentity_33:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_33n
Identity_34IdentityRestoreV2:tensors:34"/device:CPU:0*
T0*
_output_shapes
:2
Identity_34¨
AssignVariableOp_34AssignVariableOp assignvariableop_34_dense_3_biasIdentity_34:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_34n
Identity_35IdentityRestoreV2:tensors:35"/device:CPU:0*
T0*
_output_shapes
:2
Identity_35ª
AssignVariableOp_35AssignVariableOp"assignvariableop_35_dense_4_kernelIdentity_35:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_35n
Identity_36IdentityRestoreV2:tensors:36"/device:CPU:0*
T0*
_output_shapes
:2
Identity_36¨
AssignVariableOp_36AssignVariableOp assignvariableop_36_dense_4_biasIdentity_36:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_36n
Identity_37IdentityRestoreV2:tensors:37"/device:CPU:0*
T0*
_output_shapes
:2
Identity_37ª
AssignVariableOp_37AssignVariableOp"assignvariableop_37_dense_5_kernelIdentity_37:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_37n
Identity_38IdentityRestoreV2:tensors:38"/device:CPU:0*
T0*
_output_shapes
:2
Identity_38¨
AssignVariableOp_38AssignVariableOp assignvariableop_38_dense_5_biasIdentity_38:output:0"/device:CPU:0*
_output_shapes
 *
dtype02
AssignVariableOp_389
NoOpNoOp"/device:CPU:0*
_output_shapes
 2
NoOp¸
Identity_39Identityfile_prefix^AssignVariableOp^AssignVariableOp_1^AssignVariableOp_10^AssignVariableOp_11^AssignVariableOp_12^AssignVariableOp_13^AssignVariableOp_14^AssignVariableOp_15^AssignVariableOp_16^AssignVariableOp_17^AssignVariableOp_18^AssignVariableOp_19^AssignVariableOp_2^AssignVariableOp_20^AssignVariableOp_21^AssignVariableOp_22^AssignVariableOp_23^AssignVariableOp_24^AssignVariableOp_25^AssignVariableOp_26^AssignVariableOp_27^AssignVariableOp_28^AssignVariableOp_29^AssignVariableOp_3^AssignVariableOp_30^AssignVariableOp_31^AssignVariableOp_32^AssignVariableOp_33^AssignVariableOp_34^AssignVariableOp_35^AssignVariableOp_36^AssignVariableOp_37^AssignVariableOp_38^AssignVariableOp_4^AssignVariableOp_5^AssignVariableOp_6^AssignVariableOp_7^AssignVariableOp_8^AssignVariableOp_9^NoOp"/device:CPU:0*
T0*
_output_shapes
: 2
Identity_39«
Identity_40IdentityIdentity_39:output:0^AssignVariableOp^AssignVariableOp_1^AssignVariableOp_10^AssignVariableOp_11^AssignVariableOp_12^AssignVariableOp_13^AssignVariableOp_14^AssignVariableOp_15^AssignVariableOp_16^AssignVariableOp_17^AssignVariableOp_18^AssignVariableOp_19^AssignVariableOp_2^AssignVariableOp_20^AssignVariableOp_21^AssignVariableOp_22^AssignVariableOp_23^AssignVariableOp_24^AssignVariableOp_25^AssignVariableOp_26^AssignVariableOp_27^AssignVariableOp_28^AssignVariableOp_29^AssignVariableOp_3^AssignVariableOp_30^AssignVariableOp_31^AssignVariableOp_32^AssignVariableOp_33^AssignVariableOp_34^AssignVariableOp_35^AssignVariableOp_36^AssignVariableOp_37^AssignVariableOp_38^AssignVariableOp_4^AssignVariableOp_5^AssignVariableOp_6^AssignVariableOp_7^AssignVariableOp_8^AssignVariableOp_9*
T0*
_output_shapes
: 2
Identity_40"#
identity_40Identity_40:output:0*³
_input_shapes¡
: :::::::::::::::::::::::::::::::::::::::2$
AssignVariableOpAssignVariableOp2(
AssignVariableOp_1AssignVariableOp_12*
AssignVariableOp_10AssignVariableOp_102*
AssignVariableOp_11AssignVariableOp_112*
AssignVariableOp_12AssignVariableOp_122*
AssignVariableOp_13AssignVariableOp_132*
AssignVariableOp_14AssignVariableOp_142*
AssignVariableOp_15AssignVariableOp_152*
AssignVariableOp_16AssignVariableOp_162*
AssignVariableOp_17AssignVariableOp_172*
AssignVariableOp_18AssignVariableOp_182*
AssignVariableOp_19AssignVariableOp_192(
AssignVariableOp_2AssignVariableOp_22*
AssignVariableOp_20AssignVariableOp_202*
AssignVariableOp_21AssignVariableOp_212*
AssignVariableOp_22AssignVariableOp_222*
AssignVariableOp_23AssignVariableOp_232*
AssignVariableOp_24AssignVariableOp_242*
AssignVariableOp_25AssignVariableOp_252*
AssignVariableOp_26AssignVariableOp_262*
AssignVariableOp_27AssignVariableOp_272*
AssignVariableOp_28AssignVariableOp_282*
AssignVariableOp_29AssignVariableOp_292(
AssignVariableOp_3AssignVariableOp_32*
AssignVariableOp_30AssignVariableOp_302*
AssignVariableOp_31AssignVariableOp_312*
AssignVariableOp_32AssignVariableOp_322*
AssignVariableOp_33AssignVariableOp_332*
AssignVariableOp_34AssignVariableOp_342*
AssignVariableOp_35AssignVariableOp_352*
AssignVariableOp_36AssignVariableOp_362*
AssignVariableOp_37AssignVariableOp_372*
AssignVariableOp_38AssignVariableOp_382(
AssignVariableOp_4AssignVariableOp_42(
AssignVariableOp_5AssignVariableOp_52(
AssignVariableOp_6AssignVariableOp_62(
AssignVariableOp_7AssignVariableOp_72(
AssignVariableOp_8AssignVariableOp_82(
AssignVariableOp_9AssignVariableOp_9:C ?

_output_shapes
: 
%
_user_specified_namefile_prefix
°
ª
B__inference_dense_4_layer_call_and_return_conditional_losses_13558

inputs"
matmul_readvariableop_resource#
biasadd_readvariableop_resource
identity
MatMul/ReadVariableOpReadVariableOpmatmul_readvariableop_resource* 
_output_shapes
:
*
dtype02
MatMul/ReadVariableOpt
MatMulMatMulinputsMatMul/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
MatMul
BiasAdd/ReadVariableOpReadVariableOpbiasadd_readvariableop_resource*
_output_shapes	
:*
dtype02
BiasAdd/ReadVariableOp
BiasAddBiasAddMatMul:product:0BiasAdd/ReadVariableOp:value:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2	
BiasAddY
ReluReluBiasAdd:output:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2
Relug
IdentityIdentityRelu:activations:0*
T0*(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ2

Identity"
identityIdentity:output:0*/
_input_shapes
:ÿÿÿÿÿÿÿÿÿ:::P L
(
_output_shapes
:ÿÿÿÿÿÿÿÿÿ
 
_user_specified_nameinputs"¸L
saver_filename:0StatefulPartitionedCall_1:0StatefulPartitionedCall_28"
saved_model_main_op

NoOp*>
__saved_model_init_op%#
__saved_model_init_op

NoOp*ü	
serving_defaultè	
*
askSize
serving_default_askSize:0 
;
	askVector.
serving_default_askVector:0ÿÿÿÿÿÿÿÿÿ
*
bidSize
serving_default_bidSize:0 
;
	bidVector.
serving_default_bidVector:0ÿÿÿÿÿÿÿÿÿ
<
contractHoldings(
"serving_default_contractHoldings:0 
L
finalFundamentalEstimate0
*serving_default_finalFundamentalEstimate:0 
*
latency
serving_default_latency:0	 
8
marketHoldings&
 serving_default_marketHoldings:0 
:
numTransactions'
!serving_default_numTransactions:0 
6
omegaRatioAsk%
serving_default_omegaRatioAsk:0 
6
omegaRatioBid%
serving_default_omegaRatioBid:0 
0

privateAsk"
serving_default_privateAsk:0 
0

privateBid"
serving_default_privateBid:0 
$
side
serving_default_side:0 
(
spread
serving_default_spread:0 
@
timeSinceLastTrade*
$serving_default_timeSinceLastTrade:0	 
0

timeTilEnd"
serving_default_timeTilEnd:0	 
M
transactionHistory7
$serving_default_transactionHistory:0ÿÿÿÿÿÿÿÿÿ-
output_0!
StatefulPartitionedCall:0+
output_1
StatefulPartitionedCall:1 +
output_2
StatefulPartitionedCall:2 -
output_3!
StatefulPartitionedCall:3+
output_4
StatefulPartitionedCall:4 tensorflow/serving/predict:
×
critic_optimizer
actor_optimizer
online_critic
target_critic
online_actor
target_actor
buffer_size

signatures
_policy
_training_step"
_generic_user_object
6
	step

m
v"
_generic_user_object
6
step
m
v"
_generic_user_object
1
critic_layers"
_generic_user_object
1
critic_layers"
_generic_user_object
0
actor_layers"
_generic_user_object
0
actor_layers"
_generic_user_object
: 2Variable
-
serving_default"
signature_map
:	 2adam/t
J
0
1
2
3
4
5"
trackable_list_wrapper
J
0
1
2
3
4
5"
trackable_list_wrapper
:	 2adam/t
J
0
1
2
 3
!4
"5"
trackable_list_wrapper
J
#0
$1
%2
&3
'4
(5"
trackable_list_wrapper

)layer_with_weights-0
)layer-0
*layer_with_weights-1
*layer-1
+layer_with_weights-2
+layer-2
,trainable_variables
-	variables
.regularization_losses
/	keras_api
+&call_and_return_all_conditional_losses
_default_save_signature
__call__"ú
_tf_keras_sequentialÛ{"class_name": "Sequential", "name": "sequential", "trainable": true, "expects_training_arg": true, "dtype": "float32", "batch_input_shape": null, "must_restore_from_config": false, "config": {"name": "sequential", "layers": [{"class_name": "InputLayer", "config": {"batch_input_shape": {"class_name": "__tuple__", "items": [null, 28]}, "dtype": "float32", "sparse": false, "ragged": false, "name": "dense_input"}}, {"class_name": "Dense", "config": {"name": "dense", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}, {"class_name": "Dense", "config": {"name": "dense_1", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}, {"class_name": "Dense", "config": {"name": "dense_2", "trainable": true, "dtype": "float32", "units": 1, "activation": "linear", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}]}, "input_spec": {"class_name": "InputSpec", "config": {"dtype": null, "shape": null, "ndim": null, "max_ndim": null, "min_ndim": 2, "axes": {"-1": 28}}}, "build_input_shape": {"class_name": "TensorShape", "items": [null, 28]}, "is_graph_network": true, "keras_version": "2.4.0", "backend": "tensorflow", "model_config": {"class_name": "Sequential", "config": {"name": "sequential", "layers": [{"class_name": "InputLayer", "config": {"batch_input_shape": {"class_name": "__tuple__", "items": [null, 28]}, "dtype": "float32", "sparse": false, "ragged": false, "name": "dense_input"}}, {"class_name": "Dense", "config": {"name": "dense", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}, {"class_name": "Dense", "config": {"name": "dense_1", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}, {"class_name": "Dense", "config": {"name": "dense_2", "trainable": true, "dtype": "float32", "units": 1, "activation": "linear", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}]}}}

0layer_with_weights-0
0layer-0
1layer_with_weights-1
1layer-1
2layer_with_weights-2
2layer-2
3trainable_variables
4	variables
5regularization_losses
6	keras_api
+&call_and_return_all_conditional_losses
__call__"
_tf_keras_sequentialå{"class_name": "Sequential", "name": "sequential_1", "trainable": true, "expects_training_arg": true, "dtype": "float32", "batch_input_shape": null, "must_restore_from_config": false, "config": {"name": "sequential_1", "layers": [{"class_name": "InputLayer", "config": {"batch_input_shape": {"class_name": "__tuple__", "items": [null, 27]}, "dtype": "float32", "sparse": false, "ragged": false, "name": "dense_3_input"}}, {"class_name": "Dense", "config": {"name": "dense_3", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}, {"class_name": "Dense", "config": {"name": "dense_4", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}, {"class_name": "Dense", "config": {"name": "dense_5", "trainable": true, "dtype": "float32", "units": 1, "activation": "tanh", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}]}, "input_spec": {"class_name": "InputSpec", "config": {"dtype": null, "shape": null, "ndim": null, "max_ndim": null, "min_ndim": 2, "axes": {"-1": 27}}}, "build_input_shape": {"class_name": "TensorShape", "items": [null, 27]}, "is_graph_network": true, "keras_version": "2.4.0", "backend": "tensorflow", "model_config": {"class_name": "Sequential", "config": {"name": "sequential_1", "layers": [{"class_name": "InputLayer", "config": {"batch_input_shape": {"class_name": "__tuple__", "items": [null, 27]}, "dtype": "float32", "sparse": false, "ragged": false, "name": "dense_3_input"}}, {"class_name": "Dense", "config": {"name": "dense_3", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}, {"class_name": "Dense", "config": {"name": "dense_4", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}, {"class_name": "Dense", "config": {"name": "dense_5", "trainable": true, "dtype": "float32", "units": 1, "activation": "tanh", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}}]}}}
&:$	2adam/m_1/dense/kernel
 :2adam/m_1/dense/bias
):'
2adam/m_1/dense_1/kernel
": 2adam/m_1/dense_1/bias
(:&	2adam/m_1/dense_2/kernel
!:2adam/m_1/dense_2/bias
&:$	2adam/v_1/dense/kernel
 :2adam/v_1/dense/bias
):'
2adam/v_1/dense_1/kernel
": 2adam/v_1/dense_1/bias
(:&	2adam/v_1/dense_2/kernel
!:2adam/v_1/dense_2/bias
&:$	2adam/m/dense_3/kernel
 :2adam/m/dense_3/bias
':%
2adam/m/dense_4/kernel
 :2adam/m/dense_4/bias
&:$	2adam/m/dense_5/kernel
:2adam/m/dense_5/bias
&:$	2adam/v/dense_3/kernel
 :2adam/v/dense_3/bias
':%
2adam/v/dense_4/kernel
 :2adam/v/dense_4/bias
&:$	2adam/v/dense_5/kernel
:2adam/v/dense_5/bias

7_inbound_nodes

8kernel
9bias
:_outbound_nodes
;trainable_variables
<	variables
=regularization_losses
>	keras_api
+&call_and_return_all_conditional_losses
__call__"Æ
_tf_keras_layer¬{"class_name": "Dense", "name": "dense", "trainable": true, "expects_training_arg": false, "dtype": "float32", "batch_input_shape": null, "stateful": false, "must_restore_from_config": false, "config": {"name": "dense", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}, "input_spec": {"class_name": "InputSpec", "config": {"dtype": null, "shape": null, "ndim": null, "max_ndim": null, "min_ndim": 2, "axes": {"-1": 28}}}, "build_input_shape": {"class_name": "TensorShape", "items": [32, 28]}}

?_inbound_nodes

@kernel
Abias
B_outbound_nodes
Ctrainable_variables
D	variables
Eregularization_losses
F	keras_api
+&call_and_return_all_conditional_losses
__call__"Ì
_tf_keras_layer²{"class_name": "Dense", "name": "dense_1", "trainable": true, "expects_training_arg": false, "dtype": "float32", "batch_input_shape": null, "stateful": false, "must_restore_from_config": false, "config": {"name": "dense_1", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}, "input_spec": {"class_name": "InputSpec", "config": {"dtype": null, "shape": null, "ndim": null, "max_ndim": null, "min_ndim": 2, "axes": {"-1": 256}}}, "build_input_shape": {"class_name": "TensorShape", "items": [32, 256]}}

G_inbound_nodes

Hkernel
Ibias
Jtrainable_variables
K	variables
Lregularization_losses
M	keras_api
+&call_and_return_all_conditional_losses
__call__"Ì
_tf_keras_layer²{"class_name": "Dense", "name": "dense_2", "trainable": true, "expects_training_arg": false, "dtype": "float32", "batch_input_shape": null, "stateful": false, "must_restore_from_config": false, "config": {"name": "dense_2", "trainable": true, "dtype": "float32", "units": 1, "activation": "linear", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}, "input_spec": {"class_name": "InputSpec", "config": {"dtype": null, "shape": null, "ndim": null, "max_ndim": null, "min_ndim": 2, "axes": {"-1": 256}}}, "build_input_shape": {"class_name": "TensorShape", "items": [32, 256]}}
J
80
91
@2
A3
H4
I5"
trackable_list_wrapper
J
80
91
@2
A3
H4
I5"
trackable_list_wrapper
 "
trackable_list_wrapper
Î
Nlayer_metrics
Onon_trainable_variables
,trainable_variables

Players
-	variables
.regularization_losses
Qmetrics
Rlayer_regularization_losses
__call__
_default_save_signature
+&call_and_return_all_conditional_losses
'"call_and_return_conditional_losses"
_generic_user_object

S_inbound_nodes

Tkernel
Ubias
V_outbound_nodes
Wtrainable_variables
X	variables
Yregularization_losses
Z	keras_api
+&call_and_return_all_conditional_losses
__call__"É
_tf_keras_layer¯{"class_name": "Dense", "name": "dense_3", "trainable": true, "expects_training_arg": false, "dtype": "float32", "batch_input_shape": null, "stateful": false, "must_restore_from_config": false, "config": {"name": "dense_3", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}, "input_spec": {"class_name": "InputSpec", "config": {"dtype": null, "shape": null, "ndim": null, "max_ndim": null, "min_ndim": 2, "axes": {"-1": 27}}}, "build_input_shape": {"class_name": "TensorShape", "items": [1, 27]}}

[_inbound_nodes

\kernel
]bias
^_outbound_nodes
_trainable_variables
`	variables
aregularization_losses
b	keras_api
+&call_and_return_all_conditional_losses
__call__"Ë
_tf_keras_layer±{"class_name": "Dense", "name": "dense_4", "trainable": true, "expects_training_arg": false, "dtype": "float32", "batch_input_shape": null, "stateful": false, "must_restore_from_config": false, "config": {"name": "dense_4", "trainable": true, "dtype": "float32", "units": 256, "activation": "relu", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}, "input_spec": {"class_name": "InputSpec", "config": {"dtype": null, "shape": null, "ndim": null, "max_ndim": null, "min_ndim": 2, "axes": {"-1": 256}}}, "build_input_shape": {"class_name": "TensorShape", "items": [1, 256]}}

c_inbound_nodes

dkernel
ebias
ftrainable_variables
g	variables
hregularization_losses
i	keras_api
+&call_and_return_all_conditional_losses
 __call__"É
_tf_keras_layer¯{"class_name": "Dense", "name": "dense_5", "trainable": true, "expects_training_arg": false, "dtype": "float32", "batch_input_shape": null, "stateful": false, "must_restore_from_config": false, "config": {"name": "dense_5", "trainable": true, "dtype": "float32", "units": 1, "activation": "tanh", "use_bias": true, "kernel_initializer": {"class_name": "GlorotUniform", "config": {"seed": null}}, "bias_initializer": {"class_name": "Zeros", "config": {}}, "kernel_regularizer": null, "bias_regularizer": null, "activity_regularizer": null, "kernel_constraint": null, "bias_constraint": null}, "input_spec": {"class_name": "InputSpec", "config": {"dtype": null, "shape": null, "ndim": null, "max_ndim": null, "min_ndim": 2, "axes": {"-1": 256}}}, "build_input_shape": {"class_name": "TensorShape", "items": [1, 256]}}
J
T0
U1
\2
]3
d4
e5"
trackable_list_wrapper
J
T0
U1
\2
]3
d4
e5"
trackable_list_wrapper
 "
trackable_list_wrapper
°
jlayer_metrics
knon_trainable_variables
3trainable_variables

llayers
4	variables
5regularization_losses
mmetrics
nlayer_regularization_losses
__call__
+&call_and_return_all_conditional_losses
'"call_and_return_conditional_losses"
_generic_user_object
 "
trackable_list_wrapper
:	2dense/kernel
:2
dense/bias
 "
trackable_list_wrapper
.
80
91"
trackable_list_wrapper
.
80
91"
trackable_list_wrapper
 "
trackable_list_wrapper
°
olayer_metrics
pnon_trainable_variables
;trainable_variables

qlayers
<	variables
=regularization_losses
rmetrics
slayer_regularization_losses
__call__
+&call_and_return_all_conditional_losses
'"call_and_return_conditional_losses"
_generic_user_object
 "
trackable_list_wrapper
": 
2dense_1/kernel
:2dense_1/bias
 "
trackable_list_wrapper
.
@0
A1"
trackable_list_wrapper
.
@0
A1"
trackable_list_wrapper
 "
trackable_list_wrapper
°
tlayer_metrics
unon_trainable_variables
Ctrainable_variables

vlayers
D	variables
Eregularization_losses
wmetrics
xlayer_regularization_losses
__call__
+&call_and_return_all_conditional_losses
'"call_and_return_conditional_losses"
_generic_user_object
 "
trackable_list_wrapper
!:	2dense_2/kernel
:2dense_2/bias
.
H0
I1"
trackable_list_wrapper
.
H0
I1"
trackable_list_wrapper
 "
trackable_list_wrapper
°
ylayer_metrics
znon_trainable_variables
Jtrainable_variables

{layers
K	variables
Lregularization_losses
|metrics
}layer_regularization_losses
__call__
+&call_and_return_all_conditional_losses
'"call_and_return_conditional_losses"
_generic_user_object
 "
trackable_dict_wrapper
 "
trackable_list_wrapper
5
)0
*1
+2"
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
!:	2dense_3/kernel
:2dense_3/bias
 "
trackable_list_wrapper
.
T0
U1"
trackable_list_wrapper
.
T0
U1"
trackable_list_wrapper
 "
trackable_list_wrapper
³
~layer_metrics
non_trainable_variables
Wtrainable_variables
layers
X	variables
Yregularization_losses
metrics
 layer_regularization_losses
__call__
+&call_and_return_all_conditional_losses
'"call_and_return_conditional_losses"
_generic_user_object
 "
trackable_list_wrapper
": 
2dense_4/kernel
:2dense_4/bias
 "
trackable_list_wrapper
.
\0
]1"
trackable_list_wrapper
.
\0
]1"
trackable_list_wrapper
 "
trackable_list_wrapper
µ
layer_metrics
non_trainable_variables
_trainable_variables
layers
`	variables
aregularization_losses
metrics
 layer_regularization_losses
__call__
+&call_and_return_all_conditional_losses
'"call_and_return_conditional_losses"
_generic_user_object
 "
trackable_list_wrapper
!:	2dense_5/kernel
:2dense_5/bias
.
d0
e1"
trackable_list_wrapper
.
d0
e1"
trackable_list_wrapper
 "
trackable_list_wrapper
µ
layer_metrics
non_trainable_variables
ftrainable_variables
layers
g	variables
hregularization_losses
metrics
 layer_regularization_losses
 __call__
+&call_and_return_all_conditional_losses
'"call_and_return_conditional_losses"
_generic_user_object
 "
trackable_dict_wrapper
 "
trackable_list_wrapper
5
00
11
22"
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_dict_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_dict_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_dict_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_dict_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_dict_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_dict_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
 "
trackable_list_wrapper
«2¨
__inference__policy_513
Á²½
FullArgSpecÄ
args»·
jself
jfinal_fundamental_estimate
jprivate_bid
jprivate_ask
jomega_ratio_bid
jomega_ratio_ask
jside

jbid_size

jask_size
jspread
jmarket_holdings
jcontract_holdings
jnum_transactions
jtime_til_end
	jlatency
jtime_since_last_trade
j
bid_vector
j
ask_vector
jtransaction_history
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *Ã¢¿
!
finalFundamentalEstimate 


privateBid 


privateAsk 

omegaRatioBid 

omegaRatioAsk 


side 

bidSize 

askSize 

spread 

marketHoldings 

contractHoldings 

numTransactions 


timeTilEnd 	

latency 	

timeSinceLastTrade 	

	bidVectorÿÿÿÿÿÿÿÿÿ

	askVectorÿÿÿÿÿÿÿÿÿ
(%
transactionHistoryÿÿÿÿÿÿÿÿÿ
Î2Ë
__inference__training_step_7508§
²
FullArgSpec"
args
jself
jtransitions
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
B
#__inference_signature_wrapper_12756askSize	askVectorbidSize	bidVectorcontractHoldingsfinalFundamentalEstimatelatencymarketHoldingsnumTransactionsomegaRatioAskomegaRatioBid
privateAsk
privateBidsidespreadtimeSinceLastTrade
timeTilEndtransactionHistory
â2ß
E__inference_sequential_layer_call_and_return_conditional_losses_13184
E__inference_sequential_layer_call_and_return_conditional_losses_13266
E__inference_sequential_layer_call_and_return_conditional_losses_13242
E__inference_sequential_layer_call_and_return_conditional_losses_13160À
·²³
FullArgSpec1
args)&
jself
jinputs

jtraining
jmask
varargs
 
varkw
 
defaults
p 

 

kwonlyargs 
kwonlydefaultsª 
annotationsª *
 
â2ß
 __inference__wrapped_model_12781º
²
FullArgSpec
args 
varargsjargs
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª **¢'
%"
dense_inputÿÿÿÿÿÿÿÿÿ
ö2ó
*__inference_sequential_layer_call_fn_13283
*__inference_sequential_layer_call_fn_13300
*__inference_sequential_layer_call_fn_13201
*__inference_sequential_layer_call_fn_13218À
·²³
FullArgSpec1
args)&
jself
jinputs

jtraining
jmask
varargs
 
varkw
 
defaults
p 

 

kwonlyargs 
kwonlydefaultsª 
annotationsª *
 
ê2ç
G__inference_sequential_1_layer_call_and_return_conditional_losses_13325
G__inference_sequential_1_layer_call_and_return_conditional_losses_13350
G__inference_sequential_1_layer_call_and_return_conditional_losses_13409
G__inference_sequential_1_layer_call_and_return_conditional_losses_13434À
·²³
FullArgSpec1
args)&
jself
jinputs

jtraining
jmask
varargs
 
varkw
 
defaults
p 

 

kwonlyargs 
kwonlydefaultsª 
annotationsª *
 
þ2û
,__inference_sequential_1_layer_call_fn_13367
,__inference_sequential_1_layer_call_fn_13384
,__inference_sequential_1_layer_call_fn_13451
,__inference_sequential_1_layer_call_fn_13468À
·²³
FullArgSpec1
args)&
jself
jinputs

jtraining
jmask
varargs
 
varkw
 
defaults
p 

 

kwonlyargs 
kwonlydefaultsª 
annotationsª *
 
ê2ç
@__inference_dense_layer_call_and_return_conditional_losses_13479¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
Ï2Ì
%__inference_dense_layer_call_fn_13488¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
ì2é
B__inference_dense_1_layer_call_and_return_conditional_losses_13499¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
Ñ2Î
'__inference_dense_1_layer_call_fn_13508¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
ì2é
B__inference_dense_2_layer_call_and_return_conditional_losses_13518¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
Ñ2Î
'__inference_dense_2_layer_call_fn_13527¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
ì2é
B__inference_dense_3_layer_call_and_return_conditional_losses_13538¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
Ñ2Î
'__inference_dense_3_layer_call_fn_13547¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
ì2é
B__inference_dense_4_layer_call_and_return_conditional_losses_13558¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
Ñ2Î
'__inference_dense_4_layer_call_fn_13567¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
ì2é
B__inference_dense_5_layer_call_and_return_conditional_losses_13578¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 
Ñ2Î
'__inference_dense_5_layer_call_fn_13587¢
²
FullArgSpec
args
jself
jinputs
varargs
 
varkw
 
defaults
 

kwonlyargs 
kwonlydefaults
 
annotationsª *
 ¼
__inference__policy_513 TU\]deÏ¢Ë
Ã¢¿
!
finalFundamentalEstimate 


privateBid 


privateAsk 

omegaRatioBid 

omegaRatioAsk 


side 

bidSize 

askSize 

spread 

marketHoldings 

contractHoldings 

numTransactions 


timeTilEnd 	

latency 	

timeSinceLastTrade 	

	bidVectorÿÿÿÿÿÿÿÿÿ

	askVectorÿÿÿÿÿÿÿÿÿ
(%
transactionHistoryÿÿÿÿÿÿÿÿÿ
ª "C¢@
	
0


1 


2 
	
3


4 
__inference__training_step_7508ó&TU\]de89@AHI#$% &!'"(	«¢§
¢


transitions/0 

transitions/1 

transitions/2 

transitions/3 

transitions/4 	
ª "¢


0 


1 
 __inference__wrapped_model_12781q89@AHI4¢1
*¢'
%"
dense_inputÿÿÿÿÿÿÿÿÿ
ª "1ª.
,
dense_2!
dense_2ÿÿÿÿÿÿÿÿÿ¤
B__inference_dense_1_layer_call_and_return_conditional_losses_13499^@A0¢-
&¢#
!
inputsÿÿÿÿÿÿÿÿÿ
ª "&¢#

0ÿÿÿÿÿÿÿÿÿ
 |
'__inference_dense_1_layer_call_fn_13508Q@A0¢-
&¢#
!
inputsÿÿÿÿÿÿÿÿÿ
ª "ÿÿÿÿÿÿÿÿÿ£
B__inference_dense_2_layer_call_and_return_conditional_losses_13518]HI0¢-
&¢#
!
inputsÿÿÿÿÿÿÿÿÿ
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 {
'__inference_dense_2_layer_call_fn_13527PHI0¢-
&¢#
!
inputsÿÿÿÿÿÿÿÿÿ
ª "ÿÿÿÿÿÿÿÿÿ£
B__inference_dense_3_layer_call_and_return_conditional_losses_13538]TU/¢,
%¢"
 
inputsÿÿÿÿÿÿÿÿÿ
ª "&¢#

0ÿÿÿÿÿÿÿÿÿ
 {
'__inference_dense_3_layer_call_fn_13547PTU/¢,
%¢"
 
inputsÿÿÿÿÿÿÿÿÿ
ª "ÿÿÿÿÿÿÿÿÿ¤
B__inference_dense_4_layer_call_and_return_conditional_losses_13558^\]0¢-
&¢#
!
inputsÿÿÿÿÿÿÿÿÿ
ª "&¢#

0ÿÿÿÿÿÿÿÿÿ
 |
'__inference_dense_4_layer_call_fn_13567Q\]0¢-
&¢#
!
inputsÿÿÿÿÿÿÿÿÿ
ª "ÿÿÿÿÿÿÿÿÿ£
B__inference_dense_5_layer_call_and_return_conditional_losses_13578]de0¢-
&¢#
!
inputsÿÿÿÿÿÿÿÿÿ
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 {
'__inference_dense_5_layer_call_fn_13587Pde0¢-
&¢#
!
inputsÿÿÿÿÿÿÿÿÿ
ª "ÿÿÿÿÿÿÿÿÿ¡
@__inference_dense_layer_call_and_return_conditional_losses_13479]89/¢,
%¢"
 
inputsÿÿÿÿÿÿÿÿÿ
ª "&¢#

0ÿÿÿÿÿÿÿÿÿ
 y
%__inference_dense_layer_call_fn_13488P89/¢,
%¢"
 
inputsÿÿÿÿÿÿÿÿÿ
ª "ÿÿÿÿÿÿÿÿÿº
G__inference_sequential_1_layer_call_and_return_conditional_losses_13325oTU\]de>¢;
4¢1
'$
dense_3_inputÿÿÿÿÿÿÿÿÿ
p

 
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 º
G__inference_sequential_1_layer_call_and_return_conditional_losses_13350oTU\]de>¢;
4¢1
'$
dense_3_inputÿÿÿÿÿÿÿÿÿ
p 

 
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 ³
G__inference_sequential_1_layer_call_and_return_conditional_losses_13409hTU\]de7¢4
-¢*
 
inputsÿÿÿÿÿÿÿÿÿ
p

 
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 ³
G__inference_sequential_1_layer_call_and_return_conditional_losses_13434hTU\]de7¢4
-¢*
 
inputsÿÿÿÿÿÿÿÿÿ
p 

 
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 
,__inference_sequential_1_layer_call_fn_13367bTU\]de>¢;
4¢1
'$
dense_3_inputÿÿÿÿÿÿÿÿÿ
p

 
ª "ÿÿÿÿÿÿÿÿÿ
,__inference_sequential_1_layer_call_fn_13384bTU\]de>¢;
4¢1
'$
dense_3_inputÿÿÿÿÿÿÿÿÿ
p 

 
ª "ÿÿÿÿÿÿÿÿÿ
,__inference_sequential_1_layer_call_fn_13451[TU\]de7¢4
-¢*
 
inputsÿÿÿÿÿÿÿÿÿ
p

 
ª "ÿÿÿÿÿÿÿÿÿ
,__inference_sequential_1_layer_call_fn_13468[TU\]de7¢4
-¢*
 
inputsÿÿÿÿÿÿÿÿÿ
p 

 
ª "ÿÿÿÿÿÿÿÿÿ±
E__inference_sequential_layer_call_and_return_conditional_losses_13160h89@AHI7¢4
-¢*
 
inputsÿÿÿÿÿÿÿÿÿ
p

 
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 ±
E__inference_sequential_layer_call_and_return_conditional_losses_13184h89@AHI7¢4
-¢*
 
inputsÿÿÿÿÿÿÿÿÿ
p 

 
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 ¶
E__inference_sequential_layer_call_and_return_conditional_losses_13242m89@AHI<¢9
2¢/
%"
dense_inputÿÿÿÿÿÿÿÿÿ
p

 
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 ¶
E__inference_sequential_layer_call_and_return_conditional_losses_13266m89@AHI<¢9
2¢/
%"
dense_inputÿÿÿÿÿÿÿÿÿ
p 

 
ª "%¢"

0ÿÿÿÿÿÿÿÿÿ
 
*__inference_sequential_layer_call_fn_13201[89@AHI7¢4
-¢*
 
inputsÿÿÿÿÿÿÿÿÿ
p

 
ª "ÿÿÿÿÿÿÿÿÿ
*__inference_sequential_layer_call_fn_13218[89@AHI7¢4
-¢*
 
inputsÿÿÿÿÿÿÿÿÿ
p 

 
ª "ÿÿÿÿÿÿÿÿÿ
*__inference_sequential_layer_call_fn_13283`89@AHI<¢9
2¢/
%"
dense_inputÿÿÿÿÿÿÿÿÿ
p

 
ª "ÿÿÿÿÿÿÿÿÿ
*__inference_sequential_layer_call_fn_13300`89@AHI<¢9
2¢/
%"
dense_inputÿÿÿÿÿÿÿÿÿ
p 

 
ª "ÿÿÿÿÿÿÿÿÿÃ
#__inference_signature_wrapper_12756TU\]deé¢å
¢ 
ÝªÙ

askSize
askSize 
,
	askVector
	askVectorÿÿÿÿÿÿÿÿÿ

bidSize
bidSize 
,
	bidVector
	bidVectorÿÿÿÿÿÿÿÿÿ
-
contractHoldings
contractHoldings 
=
finalFundamentalEstimate!
finalFundamentalEstimate 

latency
latency 	
)
marketHoldings
marketHoldings 
+
numTransactions
numTransactions 
'
omegaRatioAsk
omegaRatioAsk 
'
omegaRatioBid
omegaRatioBid 
!

privateAsk

privateAsk 
!

privateBid

privateBid 

side

side 

spread
spread 
1
timeSinceLastTrade
timeSinceLastTrade 	
!

timeTilEnd

timeTilEnd 	
>
transactionHistory(%
transactionHistoryÿÿÿÿÿÿÿÿÿ"£ª

output_0
output_0

output_1
output_1 

output_2
output_2 

output_3
output_3

output_4
output_4 